/*
 * Copyright 2014-2017 Lukas Krejci
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
package org.revapi;

import java.util.Optional;

import org.revapi.configuration.Configurable;

/**
 * An element matcher is a helper extension to other extensions that need to figure out if a certain
 * element meets certain criteria.
 *
 * @author Lukas Krejci
 */
public interface ElementMatcher extends Configurable, AutoCloseable {

    /**
     * Tries to compile the provided recipe into a form that can test individual elements.
     *
     * @param recipe the recipe to compile
     *
     * @return a compiled recipe or empty optional if the string cannot be compiled by this matcher
     */
    Optional<CompiledRecipe> compile(String recipe);

    interface CompiledRecipe {

        /**
         * Decides whether given element matches this recipe.
         *
         * <p>Note that the callers need to be able to retry the elements undecidable by this recipe again after
         * the whole element tree has been processed.
         *
         *
         * @param stage
         * @param element the element to match
         * @return a match result - {@link FilterMatch#UNDECIDED} means that the decision could not be made in this round
         */
        FilterMatch test(ElementGateway.AnalysisStage stage, Element element);
    }
}