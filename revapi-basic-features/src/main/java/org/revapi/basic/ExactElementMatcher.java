/*
 * Copyright 2014-2018 Lukas Krejci
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
package org.revapi.basic;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.revapi.AnalysisContext;
import org.revapi.Element;
import org.revapi.ElementGateway;
import org.revapi.ElementMatcher;
import org.revapi.FilterMatch;
import org.revapi.FilterResult;
import org.revapi.TreeFilter;
import org.revapi.simple.RepeatingTreeFilter;

/**
 * @author Lukas Krejci
 */
public final class ExactElementMatcher implements ElementMatcher {
    @Override
    public Optional<TreeFilter> compile2(String recipe) {
        return Optional.of(new StringMatch(recipe));
    }

    @Override
    public void close() throws Exception {

    }

    @Nullable
    @Override
    public String getExtensionId() {
        return "matcher.exact";
    }

    @Nullable
    @Override
    public Reader getJSONSchema() {
        return null;
    }

    @Override
    public void initialize(@Nonnull AnalysisContext analysisContext) {

    }

    private static final class StringMatch extends RepeatingTreeFilter {
        final String match;

        private StringMatch(String match) {
            this.match = match;
        }

        @Override
        protected FilterResult doStart(Element element) {
            boolean m = match.equals(element.getFullHumanReadableString());
            return FilterResult.from(FilterMatch.fromBoolean(m), m);
        }

        @Override
        public Map<Element, FilterMatch> finish() {
            return Collections.emptyMap();
        }
    }
}