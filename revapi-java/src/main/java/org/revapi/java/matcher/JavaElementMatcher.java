/*
 * Copyright 2014-2019 Lukas Krejci
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.revapi.java.matcher;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.revapi.AnalysisContext;
import org.revapi.Element;
import org.revapi.ElementMatcher;
import org.revapi.FilterFinishResult;
import org.revapi.FilterMatch;
import org.revapi.FilterStartResult;
import org.revapi.TreeFilter;
import org.revapi.classif.Classif;
import org.revapi.classif.MatchingProgress;
import org.revapi.classif.ModelInspector;
import org.revapi.classif.StructuralMatcher;
import org.revapi.classif.TestResult;
import org.revapi.classif.WalkInstruction;
import org.revapi.java.JavaArchiveAnalyzer;
import org.revapi.java.compilation.ProbingEnvironment;
import org.revapi.java.model.JavaElementFactory;
import org.revapi.java.spi.JavaModelElement;
import org.revapi.java.spi.JavaTypeElement;
import org.revapi.java.spi.UseSite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lukas Krejci
 */
public final class JavaElementMatcher implements ElementMatcher {
    private static final Logger LOG = LoggerFactory.getLogger(JavaElementMatcher.class);

    @Override
    public Optional<CompiledRecipe> compile(String recipe) {
        try {
            StructuralMatcher matcher = Classif.compile(recipe);

            return Optional.of(archiveAnalyzer -> {
                if (!(archiveAnalyzer instanceof JavaArchiveAnalyzer)) {
                    return null;
                }

                MatchingProgress<Element> progress = matcher
                        .with(new ElementInspector((JavaArchiveAnalyzer) archiveAnalyzer));

                return new TreeFilter() {
                    @Override
                    public FilterStartResult start(Element element) {
                        if (!(element instanceof JavaModelElement)) {
                            return FilterStartResult.doesntMatch();
                        }
                        return convert(progress.start(element));
                    }

                    @Override
                    public FilterFinishResult finish(Element element) {
                        if (!(element instanceof JavaModelElement)) {
                            return FilterFinishResult.doesntMatch();
                        }
                        return FilterFinishResult.direct(convert(progress.finish(element)));
                    }

                    @Override
                    public Map<Element, FilterFinishResult> finish() {
                        return progress.finish().entrySet().stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> FilterFinishResult.direct(convert(e.getValue()))));
                    }
                };
            });
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public String getExtensionId() {
        return "matcher.java";
    }

    @Nullable
    @Override
    public Reader getJSONSchema() {
        return null;
    }

    @Override
    public void initialize(@Nonnull AnalysisContext analysisContext) {
    }

    private static FilterStartResult convert(WalkInstruction instruction) {
        return FilterStartResult.direct(convert(instruction.getTestResult()), instruction.isDescend());
    }

    private static FilterMatch convert(TestResult result) {
        switch (result) {
            case DEFERRED:
                return FilterMatch.UNDECIDED;
            case PASSED:
                return FilterMatch.MATCHES;
            case NOT_PASSED:
                return FilterMatch.DOESNT_MATCH;
            default:
                throw new IllegalArgumentException(result + " not handled.");
        }
    }

    private static JavaModelElement toJava(Element element) {
        if (!(element instanceof JavaModelElement)) {
            throw new IllegalArgumentException("Only instances of JavaModelElement can be processed by matcher.java");
        }

        return (JavaModelElement) element;
    }

    private static final class ElementInspector implements ModelInspector<Element> {
        private Elements elements;
        private Types types;
        private TypeElement javaLangObject;
        final ProbingEnvironment env;

        ElementInspector(JavaArchiveAnalyzer analyzer) {
            env = analyzer.getProbingEnvironment();
        }

        @Override
        public TypeElement getJavaLangObjectElement() {
            return getJavaLangObject();
        }

        @Override
        public javax.lang.model.element.Element toElement(Element element) {
            return toJava(element).getDeclaringElement();
        }

        @Override
        public TypeMirror toMirror(Element element) {
            return toJava(element).getModelRepresentation();
        }

        @Override
        public Set<Element> getUses(Element element) {
            if (!env.isScanningComplete()) {
                return null;
            } else if (element instanceof JavaModelElement) {
                JavaModelElement user = (JavaModelElement) element;
                while (element != null && !(element instanceof JavaTypeElement)) {
                    element = element.getParent();
                }

                if (element == null) {
                    return emptySet();
                }

                JavaTypeElement type = (JavaTypeElement) element;

                Map<UseSite.Type, Map<JavaTypeElement, Set<JavaModelElement>>> usedTypes = type.getUsedTypes();

                return usedTypes.values().stream()
                        .flatMap(m -> m.entrySet().stream())
                        .filter(e -> e.getValue().contains(user))
                        .map(Map.Entry::getKey)
                        .collect(toSet());
            } else {
                return emptySet();
            }
        }

        @Override
        public Set<Element> getUseSites(Element element) {
            if (!env.isScanningComplete()) {
                return null;
            } else if (element instanceof JavaTypeElement) {
                return ((JavaTypeElement) element).getUseSites().stream()
                        .map(UseSite::getSite)
                        .collect(toSet());
            } else {
                return emptySet();
            }
        }

        @Override
        public Element fromElement(javax.lang.model.element.Element element) {
            if (!env.isScanningComplete()) {
                return JavaElementFactory.elementFor(element, element.asType(), env, null);
            } else {
                List<javax.lang.model.element.Element> path = new ArrayList<>(3);
                javax.lang.model.element.Element current = element;
                while (current != null && !(current instanceof TypeElement)) {
                    path.add(current);
                    current = current.getEnclosingElement();
                }

                TypeElement type = (TypeElement) current;
                JavaModelElement model = env.getTypeMap().get(type);
                ListIterator<javax.lang.model.element.Element> it = path.listIterator(path.size());
                while (it.hasPrevious()) {
                    javax.lang.model.element.Element child = it.previous();
                    boolean found = false;
                    for (Element e : model.getChildren()) {
                        if (!(e instanceof JavaModelElement)) {
                            // annotations
                            continue;
                        }

                        JavaModelElement m = (JavaModelElement) e;
                        if (m.getDeclaringElement() == child) {
                            model = m;
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        return null;
                    }
                }

                return model;
            }
        }

        @Override
        public List<? extends TypeMirror> directSupertypes(TypeMirror typeMirror) {
            return getTypes().directSupertypes(typeMirror);
        }

        @Override
        public boolean overrides(ExecutableElement overrider, ExecutableElement overriden, TypeElement type) {
            return getElements().overrides(overrider, overriden, type);
        }

        public Elements getElements() {
            if (elements == null) {
                elements = env.getElementUtils();
            }
            return elements;
        }

        public Types getTypes() {
            if (types == null) {
                types = env.getTypeUtils();
            }
            return types;
        }

        public TypeElement getJavaLangObject() {
            if (javaLangObject == null) {
                javaLangObject = getElements().getTypeElement("java.lang.Object");
            }
            return javaLangObject;
        }
    }
}