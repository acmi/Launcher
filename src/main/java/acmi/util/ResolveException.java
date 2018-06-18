/*
 * Copyright (c) 2018 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Data;
import lombok.Getter;

class ResolveException extends RuntimeException {
    @Getter
    private final List<Entry> problems;

    ResolveException(List<Entry> problems) {
        super(problems.toString());
        this.problems = new ArrayList<>(problems);
    }

    @Data
    static class Entry {
        final String error;
        final String artifact;
        final String reason;

        static Entry parse(String s) {
            List<String> tokens = Arrays.stream(s.split(":"))
                    .map(String::trim)
                    .collect(Collectors.toList());
            while (tokens.size() < 3) {
                tokens.add(null);
            }
            return new Entry(tokens.get(0), tokens.get(1), tokens.get(2));
        }

        @Override
        public String toString() {
            return Stream.of(error, artifact, reason)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(": "));
        }
    }
}
