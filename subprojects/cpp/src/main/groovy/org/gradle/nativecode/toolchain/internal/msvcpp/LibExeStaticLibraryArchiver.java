/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.nativecode.toolchain.internal.msvcpp;

import org.gradle.api.internal.tasks.compile.ArgCollector;
import org.gradle.api.internal.tasks.compile.ArgWriter;
import org.gradle.api.internal.tasks.compile.CompileSpecToArguments;
import org.gradle.api.internal.tasks.compile.Compiler;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.Factory;
import org.gradle.nativecode.toolchain.internal.CommandLineCompilerArgumentsToOptionFile;
import org.gradle.nativecode.toolchain.internal.CommandLineTool;
import org.gradle.nativecode.base.internal.StaticLibraryArchiverSpec;
import org.gradle.process.internal.ExecAction;

import java.io.File;

class LibExeStaticLibraryArchiver implements Compiler<StaticLibraryArchiverSpec> {
    private final CommandLineTool<StaticLibraryArchiverSpec> commandLineTool;

    public LibExeStaticLibraryArchiver(File executable, Factory<ExecAction> execActionFactory) {
        this.commandLineTool = new CommandLineTool<StaticLibraryArchiverSpec>(executable, execActionFactory)
                .withArguments(new CommandLineCompilerArgumentsToOptionFile<StaticLibraryArchiverSpec>(ArgWriter.windowsStyleFactory(), new LibExeSpecToArguments()
        ));
    }

    public WorkResult execute(StaticLibraryArchiverSpec spec) {
        return commandLineTool.execute(spec);
    }

    private static class LibExeSpecToArguments implements CompileSpecToArguments<StaticLibraryArchiverSpec> {
        public void collectArguments(StaticLibraryArchiverSpec spec, ArgCollector collector) {
            collector.args("/OUT:" + spec.getOutputFile().getAbsolutePath());
            collector.args("/NOLOGO");
            for (File file : spec.getSource()) {
                collector.args(file.getAbsolutePath());
            }
        }
    }
}
