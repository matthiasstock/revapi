/*
 * Copyright 2014 Lukas Krejci
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
 * limitations under the License
 */

package org.revapi.java.checks;

import org.revapi.MatchReport;
import org.revapi.java.CheckBase;

/**
 * @author Lukas Krejci
 * @since 0.1
 */
public abstract class AbstractJavaCheck extends CheckBase {

    protected MatchReport.Problem createProblem(Code code,
        Object... params) {

        return createProblem(code, params, params);
    }

    protected MatchReport.Problem createProblem(Code code, Object[] params, Object... attachments) {
        return code.createProblem(configuration.getLocale(), params, attachments);
    }
}
