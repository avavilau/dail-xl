package com.epam.deltix.quantgrid.engine.compiler;

import com.epam.deltix.quantgrid.engine.test.ResultCollector;
import com.epam.deltix.quantgrid.engine.test.TestInputs;
import com.epam.deltix.quantgrid.engine.value.Period;
import com.epam.deltix.quantgrid.engine.value.PeriodSeries;
import com.epam.deltix.quantgrid.util.Doubles;
import com.epam.deltix.quantgrid.util.Strings;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.deltix.quantgrid.engine.test.TestExecutor.executeWithErrors;
import static com.epam.deltix.quantgrid.engine.test.TestExecutor.executeWithoutErrors;
import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;
import static java.lang.Math.PI;

class CompilerTest {

    @Test
    void testDim0() {
        String dsl = """
                    table A
                        [a] = 1 + 2
                        [b] = [a] + 3 + 4
                        [c] = A.COUNT() + 5
                        [d] = NA
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", 3);
        data.verify("A", "b", 10);
        data.verify("A", "c", 6);
        data.verify("A", "d", Doubles.ERROR_NA);
    }

    @Test
    void testDim1Range() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = [a] + 3 + 4
                           [c] = A.COUNT() + 5
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", 1, 2, 3, 4, 5);
        data.verify("A", "b", 8, 9, 10, 11, 12);
        data.verify("A", "c", 10, 10, 10, 10, 10);
    }

    @Test
    void testDim1Filter() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5).FILTER($ > 4)
                           [b] = [a] + 1

                    table B
                       dim [c] = A.FILTER($[a] > 4)
                           [d] = [c][b]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", 5);
        data.verify("A", "b", 6);

        data.verify("B", "c", 1);
        data.verify("B", "d", 6);
    }


    @Test
    void testScalarPromotionInAContext() {
        String dsl = """
                table A
                  dim [d] = RANGE(5)
                  [a] = 10
                  [b] = [a] + 3
                  [f] = A.FILTER([a] <= $[b] + [d])
                """;

        // a is actually a constant and expected to be promoted correctly with all Projections being removed at the end
        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "f", 5, 5, 5, 5, 5);
    }

    @Test
    void testDim2Range() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3)
                       dim [b] = RANGE(4)
                           [c] = [a] + [b]
                           [d] = ROW()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3);
        data.verify("A", "b", 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4);
        data.verify("A", "c", 2, 3, 4, 5, 3, 4, 5, 6, 4, 5, 6, 7);
        data.verify("A", "d", 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
    }

    @Test
    void testDim2FlatTables() {
        String dsl = """
                table A
                   dim [a] = RANGE(5)
                       
                table B
                   dim [b] = A.FILTER($[a] > 2)
                   dim [c] = A.FILTER($[a] > 3)
                       [d] = [b][a] + [c][a]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", 1, 2, 3, 4, 5);

        data.verify("B", "b", 3, 3, 4, 4, 5, 5);
        data.verify("B", "c", 4, 5, 4, 5, 4, 5);
        data.verify("B", "d", 7, 8, 8, 9, 9, 10);
    }

    @Test
    void testDim2NestedTables() {
        String dsl = """
                table A
                  dim [a] = RANGE(3)
                                
                table B
                  dim [a] = RANGE(2)
                  dim [b] = RANGE(5)
                  [c] = A.FILTER($[a] = [a])
                  [d] = A.FILTER($[a] = [b])
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", 1, 2, 3);
        data.verify("B", "a", 1, 1, 1, 1, 1, 2, 2, 2, 2, 2);
        data.verify("B", "b", 1, 2, 3, 4, 5, 1, 2, 3, 4, 5);

        data.verify("B", "c", 1, 1, 1, 1, 1, 1, 1, 1, 1, 1);
        data.verify("B", "d", 1, 1, 1, 0, 0, 1, 1, 1, 0, 0);
    }

    @Test
    void testDim2Filter() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3)
                       dim [b] = RANGE(4).FILTER($ > 3)
                           [c] = [a] + [b]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", 1, 2, 3);
        data.verify("A", "b", 4, 4, 4);
        data.verify("A", "c", 5, 6, 7);
    }

    @Test
    void testDim2DependentFilter() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3)
                       dim [b] = RANGE(4).FILTER($ > [a])
                           [c] = [a] + [b]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", 1, 1, 1, 2, 2, 3);
        data.verify("A", "b", 2, 3, 4, 3, 4, 4);
        data.verify("A", "c", 3, 4, 5, 5, 6, 7);
    }

    @Test
    void testFindDim0() {
        String dsl = """
                    table A
                       key [a] = 1
                           [b] = 2

                    table B
                       [x1] = 1
                       [y] = A.FIND([x1])[b]
                       
                     table C
                       dim [x] = RANGE(2)
                           [r] = A.FIND(1)
                           [b] = [r][b]
                           [c] = C.FILTER($[b] = $[r][b]).COUNT()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "y", 2);
        data.verify("C", "b", 2, 2);
    }

    @Test
    void testFind() {
        String dsl = """
                    table A
                       key dim [a] = RANGE(4)
                           key [ab] = TEXT([a] + 10)
                           [c] = [a] + 100

                    table B
                       dim [x1] = RANGE(3)
                           [x2] = TEXT([x1] + 10)
                           [y] = A.FILTER($[a] >= 2).FIND([x1], [x2])[c]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "y", Doubles.ERROR_NA, 102, 103);
    }

    @Test
    void testFindByRowNumber() {
        String dsl = """
                    table A
                       dim [a] = RANGE(4)
                           [b] = [a] + 5

                    table B
                       dim [x] = RANGE(3)
                           [y] = A.FIND([x])[b]
                           [z] = A.FILTER($[a] > 1).FIND([x])[b]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "y", 6, 7, 8);
        data.verify("B", "z", Doubles.ERROR_NA, 7, 8);
    }

    @Test
    void testIncorrectFindWithNestedKey() {
        String dsl = """
                    table A
                       key [a] = RANGE(1)
                       [b] = 1

                    table B
                       [x] = RANGE(2)
                       [y] = A.FIND([x])
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verifyError("B", "y", "The key 'a' of table 'A' must be a text or a number, but got an array");
    }

    // We need to support nested arguments for FIND. It may require some adjustment of compiler utilities as we need
    // to align all the left-side (current table) arguments together. And with the current table itself.
    @Test
    @Disabled("Not yet supported")
    void testFindWithNestedKey() {
        String dsl = """
                    table A
                       key dim [a] = RANGE(5)
                       key [b] = "key " & [a]

                    table B
                       dim [d] = RANGE(4)
                       [x] = RANGE([d])
                       [y] = "key" & [x]
                       [f] = A.FIND([x], [y])
                       [z] = SUM([f][a])
                       [f2] = A.FIND([x], "key 2").FILTER(NOT ISNA($))
                       [z2] = SUM([f][a])
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verify("B", "z", 1, 3, 6, 10);
        data.verify("B", "z2", 0, 2, 2, 2);
    }


    @Test
    void testIndex() {
        String dsl = """
                    table A
                       dim [a] = RANGE(4)
                           [b] = [a] + 5

                    table B
                       dim [x] = RANGE(3)
                           [y] = A.INDEX([x])[b]
                           [z] = A.FILTER($[a] > 1).INDEX([x])[b]
                           [w] = A.FILTER($[a] > [x]).INDEX([x])[b]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "y", 6, 7, 8);
        data.verify("B", "z", 7, 8, 9);
        data.verify("B", "w", 7, 9, Doubles.ERROR_NA);
    }

    @Test
    void testIndex2Dim() {
        String dsl = """
                    table A
                       dim [a] = RANGE(4)
                           [b] = [a] + 5

                    table B
                       dim [w] = RANGE(3)
                           [x] = A.INDEX([w])[b]
                       dim [y] = RANGE(2)
                           [z] = A.INDEX([y])[b]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "x", 6, 6, 7, 7, 8, 8);
        data.verify("B", "z", 6, 7, 6, 7, 6, 7);
    }

    @Test
    @Disabled("Not yet supported")
    void testFindAfterCartesian() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3) # 0,1,2
                       key [b] = [a] + 11 # 11,12,13

                    table B
                       dim [c] = RANGE(5)  # 0,1,2,3,4
                           [d] = [c] + 10  # 10,11,12,13,14
                           [e] = A.FILTER($[b] >= [d]).FIND([d])[a]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "e", Doubles.ERROR_NA, 0, 1, 2, Doubles.ERROR_NA);
    }

    @Test
    @Disabled("Not yet supported")
    void testFindAfterCartesianByZeroDimension() { // #bullshitgraph
        String dsl = """
                    table A
                       dim [a] = RANGE(3) # 0,1,2
                       key [b] = [a] + 11 # 11,12,13

                    table B
                       dim [c] = RANGE(5)  # 0,1,2,3,4
                           [d] = [c] + 10  # 10,11,12,13,14
                           [e] = 12
                           [f] = A.FILTER($[b] >= [d]).FIND([e])[a]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "f", 1, 1, 1, Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testDim3Range() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3)
                       dim [b] = RANGE(4)
                       dim [c] = RANGE(5)
                           [d] = [a] + [b] + [c]
                           [e] = [c] + [b] + [a]
                           [f] = [a] + [c] + [b]
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testDim3With2on1And3on1() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = A
                       
                    table B
                       dim [d] = A.FILTER(1)
                       dim [e] = A.FILTER([d][a] < $[a])
                       dim [f] = A.FILTER([d][a] = $[a])
                           [g] = [d][a]
                           [h] = [e][a]
                           [j] = [f][a]
                           [k] = [d][b]
                           [l] = [e][b]
                           [m] = [f][b]
                           [n] = [d][a] + 1
                           [o] = [e][a] + 2
                           [p] = [f][a] + 3
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testDim4Range() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3)
                       dim [b] = RANGE(4).FILTER($ > [a])
                       dim [c] = RANGE(5)
                       dim [d] = RANGE(6)
                           [f] = [c] + [d]
                           [e] = [a] + [c]
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testDim10Range() {
        String dsl = """
                    table A
                       dim [a] = RANGE(1)
                       dim [b] = RANGE(2)
                       dim [c] = RANGE(3)
                       dim [d] = RANGE(4)
                       dim [e] = RANGE(5)
                       dim [f] = RANGE(1)
                       dim [g] = RANGE(2)
                       dim [h] = RANGE(3)
                       dim [i] = RANGE(4)
                       dim [j] = RANGE(5)
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testDim1PeriodSeries() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = [a] + 10
                           [c] = [b] + 20
                       
                    table B
                       dim [d] = PERIODSERIES(A[b], A[c], "DAY")
                           [e] = [d][period]
                           [f] = [d][timestamp]
                           [g] = [d][value]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "d", "(1900-01-10, 31.0)", "(1900-01-11, 32.0)",
                "(1900-01-12, 33.0)", "(1900-01-13, 34.0)", "(1900-01-14, 35.0)");

        data.verify("B", "e", "DAY", "DAY", "DAY", "DAY", "DAY");
        data.verify("B", "f", 11, 12, 13, 14, 15);
        data.verify("B", "g", 31, 32, 33, 34, 35);
    }

    @Test
    void testDim2PeriodSeries() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = [a] + 10
                           [c] = [b] + 20
                       
                    table B
                       dim [d] = RANGE(4)
                       dim [e] = PERIODSERIES(A.FILTER([d] < $[a])[b], A.FILTER([d] < $[a])[c], "DAY")
                           [f] = [e][period]
                           [g] = [e][timestamp]
                           [h] = [e][value]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "e",
                "(1900-01-11, 32.0)", "(1900-01-12, 33.0)", "(1900-01-13, 34.0)", "(1900-01-14, 35.0)",
                "(1900-01-12, 33.0)", "(1900-01-13, 34.0)", "(1900-01-14, 35.0)", "(1900-01-13, 34.0)",
                "(1900-01-14, 35.0)", "(1900-01-14, 35.0)");

        data.verify("B", "f", "DAY", "DAY", "DAY", "DAY", "DAY", "DAY", "DAY", "DAY", "DAY", "DAY");
        data.verify("B", "g", 12, 13, 14, 15, 13, 14, 15, 14, 15, 15);
        data.verify("B", "h", 32, 33, 34, 35, 33, 34, 35, 34, 35, 35);
    }

    @Test
    void testDim3PeriodSeries() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = [a] + 10
                           [c] = [b] + 20
                       
                    table B
                       dim [d] = RANGE(4)
                       dim [e] = PERIODSERIES(A.FILTER([d] < $[a])[b], A.FILTER([d] < $[a])[c], "DAY")
                       dim [f] = A.FILTER([d] = $[a])
                           [g] = [e][period]
                           [h] = [e][timestamp]
                           [j] = [e][value]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "e",
                "(1900-01-11, 32.0)", "(1900-01-12, 33.0)", "(1900-01-13, 34.0)", "(1900-01-14, 35.0)",
                "(1900-01-12, 33.0)", "(1900-01-13, 34.0)", "(1900-01-14, 35.0)", "(1900-01-13, 34.0)",
                "(1900-01-14, 35.0)", "(1900-01-14, 35.0)");

        data.verify("B", "g",
                "DAY", "DAY", "DAY", "DAY", "DAY", "DAY", "DAY", "DAY", "DAY", "DAY");
        data.verify("B", "h",
                12, 13, 14, 15, 13, 14, 15, 14, 15, 15);
        data.verify("B", "j",
                32, 33, 34, 35, 33, 34, 35, 34, 35, 35);
    }

    @Test
    void testUnique() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3)
                           [b] = [a] + 10
                           
                    table B
                       dim [c] = A[a].UNIQUE()
                       dim [d] = A.FILTER($[a] <= [c])[b].UNIQUE()
                           [e] = [c] + [d] + 1
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "c", 1, 2, 2, 3, 3, 3);
        data.verify("B", "d", 11, 11, 12, 11, 12, 13);
        data.verify("B", "e", 13, 14, 15, 15, 16, 17);
    }

    @Test
    void testUniqueBy() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3)
                           [b] = [a] + 10
                           
                    table B
                       dim [c] = A.UNIQUEBY($[a], $[b] + 5)[a]
                       dim [d] = A.UNIQUEBY($[a] + [c])[b]
                           [e] = [c] + [d] + 1
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testSortBy() {
        String dsl = """
                    table A
                       dim [a] = RANGE(4)
                           
                    table B
                       dim [c] = A.SORTBY(-$[a])[a]
                       dim [d] = A[a].FILTER($ < [c]).SORTBY(-$)
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "c", 4, 4, 4, 3, 3, 2);
        data.verify("B", "d", 3, 2, 1, 2, 1, 1);
    }

    @Test
    void testSimpleSort() {
        String dsl = """
                    !manual()
                    table A
                       [a] = NA
                    override
                    [a]
                    "a"
                    "c"
                    "b"
                           
                    table B
                       dim [x] = SORT(A[a])
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "x", "a", "b", "c");
    }

    @Test
    void testNestedSort() {
        String dsl = """
                    !manual()
                    table A
                       [a] = NA
                       [b] = NA
                    override
                    [a],[b]
                    1,2
                    1,3
                    1,1
                    2,2
                    2,1
                    3,1
                    3,2

                    table B
                       dim [x] = RANGE(3)
                       dim [y] = SORT(A.FILTER($[a] = [x])[b])
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "x", 1, 1, 1, 2, 2, 3, 3);
        data.verify("B", "y", 1, 2, 3, 1, 2, 1, 2);
    }

    @Test
    void testSum() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           
                    table B
                       dim [b] = RANGE(6)
                           [c] = A[a].SUM()
                           [d] = A.FILTER([b] < $[a])[a].SUM()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "c", 15, 15, 15, 15, 15, 15);
        data.verify("B", "d", 14, 12, 9, 5, 0, 0);
    }

    @Test
    void testAverage() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           
                    table B
                       dim [b] = RANGE(6)
                           [c] = A[a].AVERAGE()
                           [d] = A.FILTER([b] < $[a])[a].AVERAGE()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "c", 3.0, 3.0, 3.0, 3.0, 3.0, 3.0);
        data.verify("B", "d", 3.5, 4.0, 4.5, 5.0, Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testMin() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           
                    table B
                       dim [b] = RANGE(6)
                           [c] = A[a].MIN()
                           [d] = A.FILTER([b] < $[a])[a].MIN()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "c", 1, 1, 1, 1, 1, 1);
        data.verify("B", "d", 2, 3, 4, 5, Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testMinBy() {
        String dsl = """
                    !manual()
                    table A
                       [a] = NA
                       [b] = ROW()
                    override
                    [a]
                    NA # 1
                    3  # 2
                    1  # 3
                    4  # 4

                    table B
                       dim [c] = RANGE(4)
                           [d] = A.MINBY($[a])[b]
                           [e] = A.FILTER(NOT ISNA($[a])).MINBY($[a])[b]
                           [f] = A.FILTER($[b] >= [c]).MINBY($[a])[b]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "d", 3, 3, 3, 3);
        data.verify("B", "e", 3, 3, 3, 3);
        data.verify("B", "f", 3, 3, 3, 4);
    }

    @Test
    void testMaxBy() {
        String dsl = """
                    !manual()
                    table A
                       [a] = NA
                       [b] = ROW()
                    override
                    [a]
                    NA # 1
                    3  # 2
                    4  # 3
                    1  # 4

                    table B
                       dim [c] = RANGE(4)
                           [d] = A.MAXBY($[a])[b]
                           [e] = A.FILTER(NOT ISNA($[a])).MAXBY($[a])[b]
                           [f] = A.FILTER($[b] >= [c]).MAXBY($[a])[b]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "d", 3, 3, 3, 3);
        data.verify("B", "e", 3, 3, 3, 3);
        data.verify("B", "f", 3, 3, 3, 4);
    }

    @Test
    void testMax() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           
                    table B
                       dim [b] = RANGE(6)
                           [c] = A[a].MAX()
                           [d] = A.FILTER([b] < $[a])[a].MAX()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "c", 5, 5, 5, 5, 5, 5);
        data.verify("B", "d", 5, 5, 5, 5, Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testStdevs() {
        String dsl = """
                    !manual()
                    table A
                       [a] = NA
                       override
                       [a]
                       1
                       2
                       3
                           
                    table B
                       dim [b] = RANGE(4)
                           [c] = A[a].STDEVS()
                           [d] = A.FILTER([b] < $[a])[a].STDEVS()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "c", 1.0, 1.0, 1.0, 1.0);
        data.verify("B", "d", 0.7071067811865476, 0.0, Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testStdevp() {
        String dsl = """
                    !manual()
                    table A
                       [a] = NA
                       override
                       [a]
                       1
                       2
                       3
                           
                    table B
                       dim [b] = RANGE(4)
                           [c] = A[a].STDEVP()
                           [d] = A.FILTER([b] < $[a])[a].STDEVP()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "c", 0.816496580927726, 0.816496580927726, 0.816496580927726, 0.816496580927726);
        data.verify("B", "d", 0.5, 0.0, Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testGeomean() {
        String dsl = """
                    !manual()
                    table A
                       [a] = NA
                       override
                       [a]
                       2
                       0.5
                       1
                           
                    table B
                       dim [b] = RANGE(4)
                           [c] = A[a].GEOMEAN()
                           [d] = A.FILTER([b] - 1 < $[a])[a].GEOMEAN()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "c", 1.0, 1.0, 1.0, 1.0);
        data.verify("B", "d", 1.0, 2.0, Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testMedian() {
        String dsl = """
                    !manual()
                    table A
                       [a] = NA
                       override
                       [a]
                       1
                       2
                       4
                           
                    table B
                       dim [b] = RANGE(5)
                           [c] = A[a].MEDIAN()
                           [d] = A.FILTER([b] < $[a])[a].MEDIAN()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "c", 2.0, 2.0, 2.0, 2.0, 2.0);
        data.verify("B", "d", 3.0, 4.0, 4.0, Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testFirst() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = A[a].FIRST()
                           [c] = A.FIRST()[a]
                           [d] = A.FILTER([a] < $[a])[a].FIRST()
                           [e] = A.FILTER([a] < $[a]).FIRST()[a]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "b", 1, 1, 1, 1, 1);
        data.verify("A", "c", 1, 1, 1, 1, 1);
        data.verify("A", "d", 2, 3, 4, 5, Doubles.ERROR_NA);
        data.verify("A", "e", 2, 3, 4, 5, Doubles.ERROR_NA);
    }

    @Test
    void testSingle() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = A.FILTER($[a] = 2)[a].SINGLE()
                           [c] = A.FILTER($[a] = 2).SINGLE()[a]
                           [d] = A.FILTER([a] < $[a])[a].SINGLE()
                           [e] = A.FILTER([a] < $[a]).SINGLE()[a]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "b", 2, 2, 2, 2, 2);
        data.verify("A", "c", 2, 2, 2, 2, 2);
        data.verify("A", "d", Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA, 5, Doubles.ERROR_NA);
        data.verify("A", "e", Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA, 5, Doubles.ERROR_NA);
    }

    @Test
    void testLast() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = A[a].LAST()
                           [c] = A.LAST()[a]
                           [d] = A.FILTER([a] < $[a])[a].LAST()
                           [e] = A.FILTER([a] < $[a]).LAST()[a]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "b", 5, 5, 5, 5, 5);
        data.verify("A", "c", 5, 5, 5, 5, 5);
        data.verify("A", "d", 5, 5, 5, 5, Doubles.ERROR_NA);
        data.verify("A", "e", 5, 5, 5, 5, Doubles.ERROR_NA);
    }

    @Test
    void testFirsts() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = A[a].FIRST(1).SUM()
                           [c] = A.FIRST(3)[a].SUM()
                           [d] = A.FILTER([a] < $[a])[a].FIRST(1).SUM()
                           [e] = A.FILTER([a] < $[a]).FIRST([a])[a].SUM()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "b", 1, 1, 1, 1, 1);
        data.verify("A", "c", 6, 6, 6, 6, 6);
        data.verify("A", "d", 2, 3, 4, 5, 0);
        data.verify("A", "e", 2, 7, 9, 5, 0);
    }

    @Test
    void testLasts() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = A[a].LAST(2).SUM()
                           [c] = A.LAST(1)[a].SUM()
                           [d] = A.FILTER([a] < $[a])[a].LAST(1).SUM()
                           [e] = A.FILTER([a] < $[a]).LAST([a])[a].SUM()
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "b", 9, 9, 9, 9, 9);
        data.verify("A", "c", 5, 5, 5, 5, 5);
        data.verify("A", "d", 5, 5, 5, 5, 0);
        data.verify("A", "e", 5, 9, 9, 5, 0);
    }

    @Test
    void testDereferenceFlatTable() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3)
                           
                    table B
                       dim [b] = A.FILTER(1)
                       
                    table C
                       dim [c] = A.FILTER(2)
                           [x] = B.FILTER([c][a] = $[b][a]).COUNT()
                           
                    table D
                       dim [d] = B.FILTER(3)
                           [y] = [d][b][a]
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testDereferenceFlatDependentTable() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3)
                           
                    table B
                       dim [b] = A.FILTER(1)
                       
                    table C
                       dim [c] = A.FILTER(2)
                       dim [d] = B.FILTER([c][a] = $[b][a])
                           [n] = [d][b]
                           
                    table D
                       dim [e] = C.FILTER($[c][a] = $[d][b][a])
                           [x] = [e][n][a]
                           [y] = [e][c][a]
                           [z] = [e][d][b][a]
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testDereferenceFlatDependentTableInNestedContext() {
        String dsl = """
                    table A
                       dim [a] = RANGE(3)
                           
                    table B
                       dim [b] = A.FILTER(1)
                       
                    table C
                       dim [c] = B.FILTER(2)[b].FILTER($[a])[a]
                       dim [d] = B.FILTER([c] = $[b][a])[b].FILTER($[a])[a]
                           [e] = [c] + [d]
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testDereferenceNestedTable() {
        String dsl = """
                    table A
                       key dim [a] = RANGE(3)
                           [b] = A.FILTER($[a] <= [a])
                           
                    table B
                       dim [c] = A
                       dim [d] = [c][b]
                           [e] = [d][a]
                """;

        executeWithoutErrors(dsl);

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "e", 1, 1, 2, 1, 2, 3);
    }

    @Test
    void testDereferenceNestedTableAfterFind() {
        String dsl = """
                    table A
                       key dim [a] = RANGE(3)
                           [b] = A.FILTER($[a] <= [a])
                           
                    table B
                       dim [c] = A.FIND(2)[b]
                           [d] = [c][a]
                """;

        executeWithoutErrors(dsl);

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "d", 1, 2);
    }

    @Test
    void testDereferenceNestedSimpleTableAfterFind() {
        String dsl = """
                    table A
                       key [a] = 0
                           [b] = RANGE(3)

                    table B
                       dim [x] = A.FIND(0)[b]
                """;

        executeWithoutErrors(dsl);

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "x", 1, 2, 3);
    }

    @Test
    void testDereferenceNestedTableWithinFormula() {
        String dsl = """
                    table A
                       key dim [a] = RANGE(3)
                           [b] = A.FILTER($[a] <= [a])
                           [c] = A.FILTER($[b].COUNT())
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verifyError("A", "c", "Cannot access array of rows [b] from another array of rows. Try flattening one of arrays using dim keyword.");
    }

    @Test
    void testInputErrors() {
        String dsl = """
                table A
                   [a] = INPUT("empty.csv")
                   [b] = INPUT("duplicated-column.csv")
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verifyError("A", "a", "The document doesn't have headers.");
        data.verifyError("A", "b", "Column names must be unique. Duplicate found: a.");
    }

    @Test
    void testInputSingleDim() {
        String dsl = """
                    table A
                       dim [a]  = INPUT("%s")
                           [f1]  = [a][DATA_DOMAIN.id] # dereference !nested field from INPUT
                           [f3]  = [a][INDICATOR.id]
                           [f7]  = [a][OBS_VALUE]
                           [b]  = [f7] + 1
                  
                    table B
                       dim [f3]  = A.UNIQUEBY($[f3])[f3]
                """.formatted(TestInputs.CPI_CSV);

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "f1", "CPI", "CPI", "CPI", "CPI", "CPI");
        data.verify("A", "b", 106.57, 105.67, 100.87, 136.987, 146.4);
    }

    @Test
    void testInputWithQuotedHeaders() {
        String dsl = """
                    table A
                      dim [source] = INPUT("country-stats-quoted.csv")
                          [country] = [source][country]
                          [date] = [source][date]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "country", "USA", "USA", "China", "China", "EU", "EU");
        data.verify("A", "date", 44197.0, 44562.0, 44197.0, 44562.0, 44197.0, 44562.0);
    }

    @Test
    void testInputJoin() {
        String dsl = """
                    table A
                      dim [a] = RANGE(3)

                      # dereference a nested column with dims
                      dim [b] = INPUT("%s").FILTER($[OBS_VALUE] = [a] AND $[DATA_DOMAIN.id] = "CPI")[OBS_VALUE]
                """.formatted(TestInputs.CPI_CSV);

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a");
        data.verify("A", "b");
    }

    @Test
    void testInputNestedField() {
        String dsl = """
                    table A
                       dim [r] = RANGE(2)
                       dim [a]  = INPUT("%s")[OBS_VALUE] # dereference nested field with empty dimensions
                           [b]  = [a] + 1
                """.formatted(TestInputs.CPI_CSV);

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "r", 1, 1, 1, 1, 1, 2, 2, 2, 2, 2);
        data.verify("A", "a", 105.57, 104.67, 99.87, 135.987, 145.4, 105.57, 104.67, 99.87, 135.987, 145.4);
        data.verify("A", "b", 106.57, 105.67, 100.87, 136.987, 146.4, 106.57, 105.67, 100.87, 136.987, 146.4);
    }

    @Test
    void testInputWithFilterMultiDim() {
        String dsl = """
                    table A
                       dim [a]   = INPUT("%s").FILTER($[OBS_VALUE] > 100) # dereference !nested column [OBS_VALUE]
                           [f1]  = [a][DATA_DOMAIN.id]
                           [f3]  = [a][INDICATOR.id]
                           [f6]  = [a][TIME_PERIOD]
                           [f7]  = [a][OBS_VALUE]
                           [bin] = [f7] + 1
                       dim [b]   = RANGE(2)
                           [x]   = [b] + [bin]

                    table B
                       dim [a]  = A.FILTER($[bin] > 110)
                           [f1] = [a][f1]
                           [f7] = [a][bin]
                       dim [b]  = RANGE(2)
                           [f4] = [b] + [f7]
                """.formatted(TestInputs.CPI_CSV);

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "bin", 106.57, 106.57, 105.67, 105.67, 136.987, 136.987, 146.4, 146.4);
        data.verify("A", "b", 1, 2, 1, 2, 1, 2, 1, 2);

        data.verify("B", "f7", 136.987, 136.987, 136.987, 136.987, 146.4, 146.4, 146.4, 146.4);
        data.verify("B", "b", 1, 2, 1, 2, 1, 2, 1, 2);
        data.verify("B", "f4", 137.987, 138.987, 137.987, 138.987, 147.4, 148.4, 147.4, 148.4);
    }

    @Test
    void testInputWithEmptyAndAnonymousColumns() {
        String dsl = """
                table A
                  dim [source] = INPUT("%s")
                
                table B
                  dim [fields] = A[source].FIELDS()
                
                table C
                  dim [source] = A[source]
                  [Column2] = [source][Column2]
                  [Column2_2] = [source][Column2_2]
                  [Column8] = [source][Column8]
                  [b] = [source][b]
                  [c] = [source][c]
                  [d] = [source][d]
                """.formatted(TestInputs.EMPTY_AND_ANONYMOUS_COLUMNS_CSV);

        ResultCollector data = executeWithoutErrors(dsl);

        data.verify("B", "fields", "Column2", "Column2_2", "Column8", "b", "c", "d");
        data.verify("C", "Column2", 0, 4);
        data.verify("C", "Column2_2", 1, Doubles.EMPTY);
        data.verify("C", "Column8", 3, Doubles.EMPTY);
        data.verify("C", "b", Strings.EMPTY, Strings.EMPTY);
        data.verify("C", "c", 2, 5);
        data.verify("C", "d", Doubles.EMPTY, 6);
    }

    @Test
    void testSimplePeriodSeries() {
        String dsl = """
                table A
                    dim [a]     = INPUT("%s")
                        [date]  = [a][date]
                        [value] = [a][value]

                table B
                    [a] = PERIODSERIES(A[date], A[value], "DAY")
                    [b] = EXTRAPOLATE([a])
                    [c] = PERCENTCHANGE([a])
                """.formatted(TestInputs.USA_GDP_SORTED_CSV);

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "a",
                new PeriodSeries(Period.DAY, 44561, 21060, 23315, Doubles.ERROR_NA, Doubles.ERROR_NA, 23316));
        data.verify("B", "b", new PeriodSeries(Period.DAY, 44561, 21060, 23315, 23315, 23315, 23316));
        data.verify("B", "c", new PeriodSeries(Period.DAY, 44562, 10.70750237416904));
    }

    @Test
    void testPivotDim() {
        String dsl = """
                    table A
                       dim [row]   = INPUT("%s")
                           [indicator]  = [row][indicator]
                           [value]  = [row][value]

                    table B
                       dim [*] = A.PIVOT($[indicator], $[value].COUNT())
                """.formatted(TestInputs.COUNTRY_INDICATORS_CSV);

        ResultCollector collector = executeWithErrors(dsl);
        collector.verifyError("B", "*", "Pivot table can't be dimension or used in formulas");
    }

    @Test
    void testPivotInFormula() {
        String dsl = """
                    table A
                       dim [row]   = INPUT("%s")
                           [indicator]  = [row][indicator]
                           [value]  = [row][value]

                    table B
                           [*] = A.PIVOT($[indicator], $[value].COUNT()).FILTER($[IR] > 5)
                """.formatted(TestInputs.COUNTRY_INDICATORS_CSV);

        ResultCollector collector = executeWithErrors(dsl);
        collector.verifyError("B", "*", "Pivot table can't be dimension or used in formulas");
    }

    @Test
    void testPivot() {
        String dsl = """
                    table A
                       dim [a]   = INPUT("%s")
                           [country]  = [a][country]
                           [date]  = [a][date]
                           [indicator]  = [a][indicator]
                           [value]  = [a][value]

                    table B
                       dim [a] = A.UNIQUEBY($[country], $[date])
                           [country] = [a][country]
                           [date] = [a][date]
                           [row] = A.FILTER([country] = $[country] AND [date] = $[date])
                           [*]   = [row].PIVOT($[indicator], B.COUNT() + $[value].COUNT())
                           [GDP Percent Change] = [GDP] + 1
                           [IR2] = [*][IR] + 1
                           [e] = [MISSING] + 1

                    table C
                        dim [a] = A.UNIQUEBY($[country])
                            [country] = [a][country]
                            [b] = B.FILTER([country] = $[country] AND $[GDP] = 7)[IR]
                            [c] = [b].COUNT()
                """.formatted(TestInputs.COUNTRY_INDICATORS_CSV);

        ResultCollector data = executeWithErrors(dsl);

        data.verify("A", "country",
                "USA", "China", "EU", "USA", "China", "EU", "USA", "China", "EU", "USA", "China", "EU");
        data.verify("A", "date",
                44562, 44562, 44562, 44197, 44197, 44197, 44562, 44562, 44562, 44197, 44197, 44197);

        data.verify("B", "country", "USA", "China", "EU", "USA", "China", "EU");
        data.verify("B", "date", 44562, 44562, 44562, 44197, 44197, 44197);

        data.verify("B", "GDP Percent Change", 8, 8, 8, 8, 8, 8);
        data.verify("B", "IR2", 8, 8, 8, 8, 8, 8);
        data.verifyError("B", "e", "The column 'MISSING' does not exist in the pivot table.");

        data.verify("C", "c", 2, 2, 2);
    }

    @Test
    void testPivotPromoted() {
        String dsl = """
                    table A
                       dim [a]   = INPUT("%s")
                           [country]  = [a][country]
                           [date]  = [a][date]
                           [indicator]  = [a][indicator]
                           [value]  = [a][value]

                    table B
                       dim [n] = RANGE(2)
                       dim [a] = A.UNIQUEBY($[country], $[date])
                           [country] = [a][country]
                           [date] = [a][date]
                           [*] = A.FILTER([country] = $[country] AND [date] = $[date]).PIVOT($[indicator], COUNT($[value]))
                           [GDP Percent Change] = [GDP] + 1
                           [IR2] = [*][IR] + 1
                """.formatted(TestInputs.COUNTRY_INDICATORS_CSV);

        ResultCollector data = executeWithoutErrors(dsl);

        data.verify("A", "country",
                "USA", "China", "EU", "USA", "China", "EU", "USA", "China", "EU", "USA", "China", "EU");
        data.verify("A", "date",
                44562, 44562, 44562, 44197, 44197, 44197, 44562, 44562, 44562, 44197, 44197, 44197);

        data.verify("B", "country", "USA", "China", "EU", "USA", "China", "EU",
                "USA", "China", "EU", "USA", "China", "EU");
        data.verify("B", "date", 44562, 44562, 44562, 44197, 44197, 44197,
                44562, 44562, 44562, 44197, 44197, 44197);

        data.verify("B", "GDP Percent Change", 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2);
        data.verify("B", "IR2", 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2);
    }

    @Test
    void testPivotWithPeriodSeries() {
        String dsl = """
                    table A
                       dim [a]         = INPUT("%s")
                           [country]   = [a][country]
                           [date]      = [a][date]
                           [indicator] = [a][indicator]
                           [value]     = [a][value]

                    table B
                       dim [a]       = A.UNIQUEBY($[country])
                           [country] = [a][country]
                           [row]     = A.FILTER([country] = $[country])
                           [*]       = [row].PIVOT($[indicator], PERIODSERIES($[date], $[value], "YEAR"))
                           [GDP_PS]  = [GDP]
                           [IR_PS]   = [IR]
                           [e]       = [MISSING]
                """.formatted(TestInputs.COUNTRY_INDICATORS_SORTED_CSV);

        ResultCollector data = executeWithErrors(dsl);
        data.verify("B", "GDP_PS",
                new PeriodSeries(Period.YEAR, 121, 21060, 23315),
                new PeriodSeries(Period.YEAR, 121, 14688, 17734),
                new PeriodSeries(Period.YEAR, 121, 13085, 14563));
        data.verify("B", "IR_PS",
                new PeriodSeries(Period.YEAR, 121, 5, 4.9),
                new PeriodSeries(Period.YEAR, 121, 0.1, 0.2),
                new PeriodSeries(Period.YEAR, 121, 7, 6.1));
        data.verifyError("B", "e", "The column 'MISSING' does not exist in the pivot table.");
    }

    @Test
    void testPivotWithErrors() {
        String dsl = """
                    table A
                       dim [a]         = INPUT("%s")
                           [country]   = [a][country]
                           [date]      = [a][date]
                           [indicator] = [a][indicator]
                           [value]     = [a][value]

                    table B
                       dim [a]                  = A.UNIQUEBY($[country], $[date])
                           [country]            = [a][country]
                           [date]               = [a][date]
                           [row]                = A.FILTER([country] = $[country] AND [date] = $[date])
                           [*]                  = [row].PIVOT($[indicator], COUNT($[value]))
                           [GDP Percent Change] = [GDP] + 1
                           [e]                  = [MISSING] + 1
                """.formatted(TestInputs.COUNTRY_INDICATORS_CSV);

        ResultCollector data = executeWithErrors(dsl);
        data.verify("B", "GDP Percent Change", 2, 2, 2, 2, 2, 2);
        data.verifyError("B", "e", "The column 'MISSING' does not exist in the pivot table.");
    }

    @Test
    void testSimplePivot() {
        String dsl = """
                    table A
                       [*] = INPUT("%s").PIVOT($[indicator], COUNT($[value]))
                       [IR2] = [IR] + 1
                       [GDP Percent Change] = [GDP] + 1
                       [e] = [MISSING] + 1
                """.formatted(TestInputs.COUNTRY_INDICATORS_CSV);

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "IR2", 7);
        data.verify("A", "GDP Percent Change", 7);
        data.verifyError("A", "e", "The column 'MISSING' does not exist in the pivot table.");
    }

    @Test
    void testSimplePivotPromoted() {
        String dsl = """
                    table A
                       dim [x] = RANGE(2)
                       [*] = INPUT("%s").PIVOT($[indicator], COUNT($[value]))
                       [IR2] = [IR] + 1
                       [GDP Percent Change] = [GDP] + 1
                       [e] = [MISSING] + 1
                """.formatted(TestInputs.COUNTRY_INDICATORS_CSV);

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "IR2", 7, 7);
        data.verify("A", "GDP Percent Change", 7, 7);
        data.verifyError("A", "e", "The column 'MISSING' does not exist in the pivot table.");
    }

    @Test
    void testSimplePivotWithError() {
        String dsl = """
                    table A
                      [*]                   = INPUT("%s").PIVOT($[indicator], COUNT($[value]))
                      [GDP Percent Change]  = [GDP] + 1;
                      [e]                   = [MISSING] + 1;
                """.formatted(TestInputs.COUNTRY_INDICATORS_CSV);

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "GDP Percent Change", 7);
        data.verifyError("A", "e", "The column 'MISSING' does not exist in the pivot table.");
    }

    @Test
    void testSimplePivotWithRefTable() {
        String dsl = """
                    table A
                        dim [a] = INPUT("%s")
                            [indicator] = [a][indicator]
                            [value] = [a][value]

                    table B
                       [*] = A.PIVOT($[indicator], COUNT($[value]))
                       [IR2] = [IR] + 1
                       [GDP Percent Change] = [GDP] + 1;
                """.formatted(TestInputs.COUNTRY_INDICATORS_CSV);

        ResultCollector data = executeWithoutErrors(dsl);

        data.verify("B", "IR2", 6 + 1);
        data.verify("B", "GDP Percent Change", 6 + 1);
    }

    @Test
    void testFieldsRef() {
        String dsl = """
                    table A
                       dim [a] = RANGE(5)
                           [b] = [a] + 3 + 4
                           [c] = [b] * 2

                    table B
                       dim [f] = A.FIELDS().FILTER($ <> "b")
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "f", "a", "c");
    }

    @Test
    void testFieldsOnRow() {
        String dsl = """
                    table A
                       dim [a] = RANGE(1)
                           [b] = [a]
                           [c] = [b]
                       
                    table B
                       dim [f] = A.FIRST().FIELDS()
                """;
        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("B", "f", "a", "b", "c");
    }

    @Test
    void testFieldsInput() {
        String dsl = """
                    table A
                       dim [f] = INPUT("%s").FIELDS()
                """.formatted(TestInputs.CPI_CSV);
        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "f", "COMMENT", "COUNTERPART_AREA.id", "DATA_DOMAIN.id", "FREQ.id",
                "INDICATOR.id", "OBS_VALUE", "REF_AREA.id", "TIME_PERIOD");
    }

    @Test
    void testManualTable() {
        String dsl = """
                !manual()
                table A
                   [a] = NA
                   [b] = NA
                override
                [a], [b]
                "USA", 5
                "UK", 7
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", "USA", "UK");
        data.verify("A", "b", 5, 7);
    }

    @Test
    void testKeyInManualTableHasNoEffect() {
        String dsl = """
                table B
                    dim [a] = A[a]

                !manual()
                table A
                   key [a] = NA
                   [b] = NA
                   [c] = [b] + 7
                override
                key [a], [b]
                "USA", 5
                "UK", 7
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", "USA", "UK");
        data.verify("A", "b", "5.0", "7.0");
    }

    @Test
    void testOverrideWithoutKeys() {
        String dsl = """            
                table A
                   dim [a] = RANGE(3)
                       [x] = [a] + 10
                   dim [b] = RANGE(2)
                       [c] = [b] + 7
                override
                row, [a], [b]
                1, 6, 5
                2, 8, 7
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", 6, 8, 2, 2, 3, 3);
        data.verify("A", "x", 16, 18, 12, 12, 13, 13);
        data.verify("A", "b", 5, 7, 1, 2, 1, 2);
        data.verify("A", "c", 12, 14, 8, 9, 8, 9);
    }

    @Test
    void testOverrideWithMultipleKeys() {
        String dsl = """
                table A
                   key dim [a] = RANGE(5)
                   key [b] = [a] + 3
                   [c] = 7
                   [d] = 9
                override
                key [a], key [b], [c]
                1, 4, 8
                2, 5, 9
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", 1, 2, 3, 4, 5);
        data.verify("A", "b", 4, 5, 6, 7, 8);
        data.verify("A", "c", 8, 9, 7, 7, 7);
        data.verify("A", "d", 9, 9, 9, 9, 9);
    }

    @Test
    void testNaOverrides() {
        String dsl = """            
                table A
                   dim [a] = RANGE(3)
                       [x] = [a] + 10
                   dim [b] = RANGE(2)
                       [c] = [b] + 7
                override
                row, [a], [b]
                1, NA, NA
                2, NA, NA
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "a", Doubles.ERROR_NA, Doubles.ERROR_NA, 2, 2, 3, 3);
        data.verify("A", "x", Doubles.ERROR_NA, Doubles.ERROR_NA, 12, 12, 13, 13);
        data.verify("A", "b", Doubles.ERROR_NA, Doubles.ERROR_NA, 1, 2, 1, 2);
        data.verify("A", "c", Doubles.ERROR_NA, Doubles.ERROR_NA, 8, 9, 8, 9);
    }

    @Test
    void testMissingOverrides() {
        String dsl = """
                table A
                   dim [a] = RANGE(5)
                       [b] = TEXT([a])
                override
                row,[a],[b]
                2,,10
                3,11,
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", 1, 2, 11, 4, 5);
        collector.verify("A", "b", "1", "10", "11", "4", "5");
    }

    @Test
    void testCastingOverrides() {
        String dsl = """
                table A
                   dim [a] = RANGE(5)
                       [b] = [a]
                override
                row,[a],[b]
                2,"30",10
                3,11,
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", "1", "30", "11", "4", "5");
        collector.verify("A", "b", "1", "10", "11", "4", "5");
    }

    @Test
    void testManualOverrides() {
        String dsl = """
                    !manual()
                    table A
                       [a] = NA
                       [b] = TEXT([a])
                    override
                       [a],[b]
                       0,
                       1,"b"
                       2,
                        ,"c"
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", 0, 1, 2, Doubles.ERROR_NA);
        collector.verify("A", "b", "0.0", "b", "2.0", "c");
    }

    @Test
    void testInvalidOverrideKeys() {
        String dsl = """            
                table A
                   dim [a] = RANGE(3)
                       [x] = [a] + 10
                override
                key [row], [a], [x]
                0, NA, NA
                1, NA, NA
                """;

        ResultCollector data = executeWithErrors(dsl);
        Assertions.assertEquals("Missing row column in override section", data.getError("A", "a"));
        Assertions.assertEquals("Missing row column in override section", data.getError("A", "x"));
    }

    @Test
    void testKeyInOverridesHasNoEffect() {
        String dsl = """            
                table A
                   dim [a] = RANGE(3)
                       [x] = [a] + 10
                override
                row, key [a], [x]
                1, NA, NA
                2, , NA
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "a", Doubles.ERROR_NA, 2, 3);
        data.verify("A", "x", Doubles.ERROR_NA, Doubles.ERROR_NA, 13);
    }

    @Test
    void testNaOverrides2() {
        String dsl = """            
                table A
                   dim [source] = INPUT("%s")
                       [CPI] = [source][DATA_DOMAIN.id]
                override
                row, [CPI]
                1, NA
                2, NA
                """.formatted(TestInputs.CPI_CSV);

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "CPI", null, null, "CPI", "CPI", "CPI");
    }

    @Test
    void testUnpivotWithSimplePivot() {
        String dsl = """
                !manual()
                table A
                  [country] = NA
                  [population] = NA
                override
                [country], [population]
                "USA", 10
                "UK", 20
                "Spain", 30
                "USA", 40
                                
                table B
                   [*] = A.PIVOT($[country], SUM($[population]))
                   [Germany] = 100
                   
                table C
                   dim [row] = B.UNPIVOT("country", "population")
                       [country] = [row][country]
                       [population] = [row][population]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("C", "country", "Germany", "Spain", "UK", "USA");
        collector.verify("C", "population", 100, 30, 20, 50);
    }

    @Test
    void testUnpivotWithSimplePivotAndCondition() {
        String dsl = """
                !manual()
                table A
                  [country] = NA
                  [population] = NA
                override
                [country], [population]
                "USA", 10
                "UK", 20
                "Spain", 30
                "USA", 40
                                
                table B
                   [*] = A.PIVOT($[country], SUM($[population]))
                   [Germany] = 100
                   [Italy] = 200
                   [bs] = "BS"
                   
                table C
                   dim [row] = B.UNPIVOT("country", "population", $ <> "Spain" AND $ <> "bs")
                       [country] = [row][country]
                       [population] = [row][population]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("C", "country", "Germany", "Italy", "UK", "USA");
        collector.verify("C", "population", 100, 200, 20, 50);
    }

    @Test
    void testUnpivotWithSimplePivotAndPromotion() {
        String dsl = """
                !manual()
                table A
                  [country] = NA
                  [population] = NA
                override
                [country], [population]
                "USA", 10
                "UK", 20
                "Spain", 30
                "USA", 40
                                
                table B
                   dim [ignore] = RANGE(1)
                       [*] = A.PIVOT($[country], SUM($[population]))
                       [Germany] = 100
                   
                table C
                   dim [row] = B.UNPIVOT("country", "population", $ <> "ignore")
                       [country] = [row][country]
                       [population] = [row][population]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("C", "country", "Germany", "Spain", "UK", "USA");
        collector.verify("C", "population", 100, 30, 20, 50);
    }

    @Test
    void testUnpivotWithNestedPivotAndCondition() {
        String dsl = """
                !manual()
                table A
                  [country] = NA
                  [indicator] = NA
                  [population] = NA
                override
                [country], [indicator], [population]
                "USA", "IR", 10
                "UK", "GDP", 20
                "Spain", "IR", 30
                "USA", "GDP", 40
                "USA", "GDP", 50
                                
                table B
                   dim [country] = UNIQUE(A[country])
                       [*] = A.FILTER([country] = $[country]).PIVOT($[indicator], SUM($[population]))
                       [ABC] = [IR] + 100
                   
                table C
                   dim [row] = B.UNPIVOT("indicator", "value", $ <> "country")
                       [country] = [row][country]
                       [indicator] = [row][indicator]
                       [value] = [row][value]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("C", "country",
                "USA", "USA", "USA", "UK", "UK", "UK", "Spain", "Spain", "Spain");

        collector.verify("C", "indicator", "ABC", "GDP", "IR", "ABC", "GDP", "IR", "ABC", "GDP", "IR");
        collector.verify("C", "value", 110, 90, 10, 100, 20, Doubles.EMPTY, 130, Doubles.EMPTY, 30);
    }

    @Test
    void testUnpivotWithoutPivot() {
        String dsl = """
                    table A
                       dim [row]     = INPUT("%s")
                           [country] = [row][country]
                           [date]    = [row][date]
                           [GDP]     = [row][GDP]
                           [IR]      = [row][IR]

                    table B
                       dim [row]       = A.UNPIVOT("indicator", "value", $ = "GDP" OR  $ = "IR")
                           [country]   = [row][country]
                           [date]      = [row][date]
                           [indicator] = [row][indicator]
                           [value]     = [row][value]
                """.formatted(TestInputs.COUNTRY_STATS_CSV);

        ResultCollector data = executeWithoutErrors(dsl);

        data.verify("B", "country",
                "USA", "USA", "USA", "USA", "China", "China", "China", "China", "EU", "EU", "EU", "EU");
        data.verify("B", "date",
                44197, 44197, 44562, 44562, 44197, 44197, 44562, 44562, 44197, 44197, 44562, 44562);
        data.verify("B", "indicator",
                "GDP", "IR", "GDP", "IR", "GDP", "IR", "GDP", "IR", "GDP", "IR", "GDP", "IR");
        data.verify("B", "value",
                21060, Doubles.EMPTY, 23315, 4.9, 14688, 0.1, 17734, 0.2, 13085, 7, Doubles.EMPTY, 6.1);
    }

    @Test
    void testUnpivotWithFilter() {
        String dsl = """
                    table A
                       dim [row]     = INPUT("%s")
                           [country] = [row][country]
                           [date]    = [row][date]
                           [GDP]     = [row][GDP]
                           [IR]      = [row][IR]

                    table B
                       dim [row]       = A.FILTER(1).UNPIVOT("indicator", "value", $ = "GDP" OR  $ = "IR").FILTER($[value] > 0)
                           [country]   = [row][country]
                           [date]      = [row][date]
                           [indicator] = [row][indicator]
                           [value]     = [row][value]
                """.formatted(TestInputs.COUNTRY_STATS_CSV);

        ResultCollector data = executeWithoutErrors(dsl);

        data.verify("B", "country", "USA", "USA", "USA", "China", "China", "China", "China", "EU", "EU",
                "EU");
        data.verify("B", "date", 44197, 44562, 44562, 44197, 44197, 44562, 44562, 44197, 44197, 44562);
        data.verify("B", "indicator", "GDP", "GDP", "IR", "GDP", "IR", "GDP", "IR", "GDP", "IR", "IR");
        data.verify("B", "value", 21060, 23315, 4.9, 14688, 0.1, 17734, 0.2, 13085, 7, 6.1);
    }

    @Test
    void testUnpivotWithCurrent() {
        String dsl = """
                    table A
                       dim [row]     = INPUT("%s")
                           [country] = [row][country]
                           [date]    = [row][date]
                           [GDP]     = [row][GDP]
                           [IR]      = [row][IR]

                    table B
                       dim [x]         = RANGE(2)
                       dim [row]       = A.FILTER([x] = 1).UNPIVOT("indicator", "value", $ = "GDP" OR  $ = "IR")
                           [country]   = [row][country]
                           [date]      = [row][date]
                           [indicator] = [row][indicator]
                           [value]     = [row][value]
                """.formatted(TestInputs.COUNTRY_STATS_CSV);

        ResultCollector data = executeWithoutErrors(dsl);

        data.verify("B", "country",
                "USA", "USA", "USA", "USA", "China", "China", "China", "China", "EU", "EU", "EU", "EU");
        data.verify("B", "date",
                44197, 44197, 44562, 44562, 44197, 44197, 44562, 44562, 44197, 44197, 44562, 44562);
        data.verify("B", "indicator",
                "GDP", "IR", "GDP", "IR", "GDP", "IR", "GDP", "IR", "GDP", "IR", "GDP", "IR");
        data.verify("B", "value",
                21060, Doubles.EMPTY, 23315, 4.9, 14688, 0.1, 17734, 0.2, 13085, 7, Doubles.EMPTY, 6.1);
    }

    @Test
    void testCycleOverride() {
        String dsl = """
                table A
                    dim [a] = RANGE(3)
                        [b] = [a] + 7
                        
                table B
                    dim [x] = RANGE(5)
                    dim [y] = A.FILTER($[a] >= [x])[b]
                        [f] = A.FILTER($[a] >= [x]).COUNT()
                override
                row, [x]
                0, 5
                1, 6
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "a", 1, 2, 3);
        data.verify("A", "b", 8, 9, 10);

        Assertions.assertEquals("Cyclic dependency: B", data.getError("B", "f"));
        Assertions.assertEquals("Cyclic dependency: B", data.getError("B", "y"));
        Assertions.assertEquals("Cyclic dependency: B", data.getError("B", "x"));
    }

    @Test
    void testCycle() {
        String dsl = """
                table A
                    dim [a] = [b]
                    dim [b] = [a]
                """;

        ResultCollector data = executeWithErrors(dsl);
        Assertions.assertEquals("Cyclic dependency: A[a]", data.getError("A", "a"));
        Assertions.assertEquals("Cyclic dependency: A[a]", data.getError("A", "b"));
    }

    @Test
    void testDateFunction() {
        String dsl = """
                table A
                    dim [source] = RANGE(21)
                    [day] = 5 * ([source] - 11)
                    [month] = [source] - 11
                    [year] = 2020
                    [date] = DATE([year], [month], [day])
                    [date2] = DATE(2020, [month], [day])
                    [invalidDate] = DATE([year], [month])
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "year", 2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020,
                2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020, 2020);
        data.verify("A", "month", -10, -9, -8, -7, -6, -5, -4, -3, -2, -1,
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        data.verify("A", "day", -50, -45, -40, -35, -30, -25, -20, -15, -10, -5, 0, 5,
                10, 15, 20, 25, 30, 35, 40, 45, 50);

        data.verify("A", "date", 43446, 43479, 43515, 43550, 43586, 43621, 43657, 43693, 43728, 43764, 43799, 43835,
                43871, 43905, 43941, 43976, 44012, 44047, 44083, 44119, 44154);

        data.verify("A", "date2", 43446, 43479, 43515, 43550, 43586, 43621, 43657, 43693, 43728, 43764, 43799, 43835,
                43871, 43905, 43941, 43976, 44012, 44047, 44083, 44119, 44154);

        Assertions.assertEquals(
                "Function DATE expects 3 arguments - \"year\", \"month\" and \"day\", but 2 were provided",
                data.getError("A", "invalidDate"));
    }

    @Test
    void testDateTimePartFunctions() {
        String dsl = """
                    table A
                       dim [row]          = INPUT("%s")
                           [date]         = [row][date]
                           [text]         = "44587"
                           [year]         = YEAR([date])
                           [invalidYear]  = YEAR([text])
                           [month]        = MONTH([date])
                           [invalidMonth] = MONTH([text])
                           [day]          = DAY([date])
                           [invalidDay]   = DAY()
                           [hour]         = HOUR([date])
                           [invalidHour]  = HOUR([date], [text], "2020")
                           [minute]       = MINUTE([date])
                           [second]       = SECOND([date])
                           
                """.formatted(TestInputs.DATE_TIME_CSV);

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "year", 2020, 2023, 1988, 1999, 2000, 2005);
        data.verify("A", "month", 3, 12, 1, 7, 5, 8);
        data.verify("A", "day", 21, 7, 3, 29, 30, 15);
        data.verify("A", "hour", 0, 17, 12, 22, 3, 23);
        data.verify("A", "minute", 37, 5, 40, 59, 0, 59);
        data.verify("A", "second", 28, 57, 3, 0, 59, 1);

        Assertions.assertEquals("Invalid argument \"date\" for function YEAR: expected a date, but got a text.",
                data.getError("A", "invalidYear"));
        Assertions.assertEquals("Invalid argument \"date\" for function MONTH: expected a date, but got a text.",
                data.getError("A", "invalidMonth"));
        Assertions.assertEquals("Function DAY expects 1 argument - \"date\", but 0 were provided",
                data.getError("A", "invalidDay"));
        Assertions.assertEquals("Function HOUR expects 1 argument - \"date\", but 3 were provided",
                data.getError("A", "invalidHour"));
    }

    @Test
    void testConcatenate() {
        String dsl = """
                table A
                    dim [source] = RANGE(5)
                        [a] = [source] - 1
                        [b] = DATE(2020, 1, [a])
                        [c] = "AND"
                        [d] = 2.7
                        [e] = [a] > [d]
                        [concat] = CONCAT([a], " ", [b], [c], [d], " ", [e])
                        [concatenate] = CONCATENATE([a], " ", [b], [c], [d], " ", [e])
                        [concat2] = CONCAT([a])
                        [invalidConcat] = CONCAT()
                        [operator] = TEXT([a]) & [c]
                    override
                    row, [b]
                    1, NA
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "concat", Strings.ERROR_NA, "1 43831.0AND2.7 FALSE", "2 43832.0AND2.7 FALSE",
                "3 43833.0AND2.7 TRUE", "4 43834.0AND2.7 TRUE");
        data.verify("A", "concatenate", Strings.ERROR_NA, "1 43831.0AND2.7 FALSE", "2 43832.0AND2.7 FALSE",
                "3 43833.0AND2.7 TRUE", "4 43834.0AND2.7 TRUE");
        data.verify("A", "concat2", "0", "1", "2", "3", "4");
        data.verify("A", "operator", "0AND", "1AND", "2AND", "3AND", "4AND");

        Assertions.assertEquals(
                "Function CONCAT expects at least 1 argument - \"values\" (repeatable), but 0 were provided",
                data.getError("A", "invalidConcat"));
    }

    @Test
    void testText() {
        String dsl = """
                    table A
                       dim [row]          = INPUT("%s")
                           [rn]           = ROW()
                           [const]        = 3
                           [bool]         = [rn] > [const]
                           [date]         = [row][date]
                           [text1]         = TEXT(3.5)
                           [text2]         = TEXT([rn])
                           [text3]         = TEXT([date], "yyyy-MM-dd")
                           [text4]         = TEXT([date], "hh:mm:ss a")
                           [text5]         = TEXT([date])
                           [text6]         = TEXT([bool])
                           [textFormatted] = TEXT([const], "yyyy-MM-dd")
                           [textInvalid2]  = TEXT()
                           [textInvalid3]  = TEXT([rn], [bool])
                           
                """.formatted(TestInputs.DATE_TIME_CSV);

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "text1", "3.5", "3.5", "3.5", "3.5", "3.5", "3.5");
        data.verify("A", "text2", "1", "2", "3", "4", "5", "6");
        data.verify("A", "text3", "2020-03-21", "2023-12-07", "1988-01-03", "1999-07-29", "2000-05-30",
                "2005-08-15");
        data.verify("A", "text4", "12:37:28 AM", "05:05:57 PM", "12:40:03 PM", "10:59:00 PM", "03:00:59 AM",
                "11:59:01 PM");
        data.verify("A", "text5", "3/21/2020 12:37:28 AM", "12/7/2023 05:05:57 PM", "1/3/1988 12:40:03 PM",
                "7/29/1999 10:59:00 PM", "5/30/2000 03:00:59 AM", "8/15/2005 11:59:01 PM");
        data.verify("A", "text6", "FALSE", "FALSE", "FALSE", "TRUE", "TRUE", "TRUE");
        data.verify("A", "textFormatted", "1900-01-02", "1900-01-02", "1900-01-02", "1900-01-02",
                "1900-01-02", "1900-01-02");

        Assertions.assertEquals(
                "Function TEXT expects from 1 to 2 arguments - \"value\" and \"format\" (optional), but 0 were provided",
                data.getError("A", "textInvalid2"));
        Assertions.assertEquals("Invalid argument \"format\" for function TEXT: constant text is expected, like \"Example\"",
                data.getError("A", "textInvalid3"));
    }

    @Test
    void testValue() {
        String dsl = """
                table A
                    dim [a] = RANGE(5)
                        [b] = TEXT([a])
                        [c] = CONCAT([b], "text")
                        [d] = VALUE([a])
                        [e] = VALUE([b])
                        [f] = VALUE([c])
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "d", 1, 2, 3, 4, 5);
        collector.verify("A", "e", 1, 2, 3, 4, 5);
        collector.verify("A", "f", Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA,
                Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testIf() {
        String dsl = """
                table A
                    dim [a] = RANGE(5)
                        [b] = [a] + 10
                        [c] = TEXT([a])
                        [d] = TEXT([b])
                        [e] = IF([a] < 3, [a], [b])
                        [f] = IF([a] < 3, [c], [d])
                        [g] = IF([a] < 3, [a], [c])
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "e", 1, 2, 13, 14, 15);
        collector.verify("A", "f", "1", "2", "13", "14", "15");
        collector.verifyError("A", "g", "IF function requires left and right arguments to have same type");
    }

    @Test
    void testIfNa() {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                    [b] = TEXT([a])
                    [c] = IFNA([a], 10)
                    [d] = IFNA([b], TEXT(10))
                    [e] = IFNA([a], [b])
                override
                [a]
                1
                2
                NA
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "c", 1, 2, 10);
        collector.verify("A", "d", "1.0", "2.0", "10");
        collector.verifyError("A", "e", "IFNA function requires source and fallback arguments to have same type");
    }

    @Test
    void testIsNa() {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                    [b] = TEXT([a])
                    [c] = ISNA([a])
                    [d] = ISNA([b])
                    [e] = A.FIND(ROW() + 1)
                    [f] = ISNA([e])
                override
                [a]
                1
                2
                NA
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "c", 0.0, 0.0, 1.0);
        collector.verify("A", "d", 0.0, 0.0, 1.0);
        collector.verify("A", "f", 0.0, 0.0, 1.0);
    }

    @Test
    void testNestedIsNa() {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                override
                [a]
                1
                2
                NA
                
                table Nested1
                    dim [a] = ISNA(A[a])
                
                table Nested2
                    dim [a] = RANGE(2)
                    dim [b] = A.FILTER([a] <> $[a] AND NOT ISNA($[a]))
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("Nested1", "a", 0, 0, 1);
        collector.verify("Nested2", "a", 1, 2);
        collector.verify("Nested2", "b", 2, 1);
    }

    @Test
    void testLog() {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                    [b] = LOG([a], 2)
                    [c] = LOG(TEXT([b]), 2)
                override
                [a]
                -1
                0
                2
                NA
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "b", Doubles.ERROR_NA, Doubles.ERROR_NA, 1.0, Doubles.ERROR_NA);
        collector.verify("A", "c", Doubles.ERROR_NA, Doubles.ERROR_NA, 0, Doubles.ERROR_NA);
    }

    @Test
    void testPow() {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                    [b] = POW([a], 2)
                    [c] = POW(TEXT([b]), 2)
                override
                [a]
                -1
                0
                2
                NA
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "b", 1, 0, 4, Doubles.ERROR_NA);
        collector.verify("A", "c", 1, 0, 16, Doubles.ERROR_NA);
    }

    @Test
    void testFalseScalarCycle() {
        String dsl = """
                    table A
                       [a] = 1
                       [b] = A.FILTER($[a]).FILTER($[a]).COUNT()
                       [c] = A.UNIQUEBY($[a]).UNIQUEBY($[b]).COUNT()
                       [d] = A.SORTBY($[b]).SORTBY($[c]).COUNT()
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testFalseCycle() {
        String dsl = """
                    table A
                      dim [a] = RANGE(1)
                          [b] = A.FILTER($[a]).FILTER($[a]).COUNT()
                          [c] = A.UNIQUEBY($[a]).UNIQUEBY($[b]).COUNT()
                          [d] = A.SORTBY($[b]).SORTBY($[c]).COUNT()
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testDuplicatedTable() {
        String dsl = """
                table A
                    dim [a] = RANGE(5)
                    [b] = [a] + 5
                
                table A
                    dim [a] = RANGE(10)
                        [b] = [a] + 7
                
                table A
                    dim [a] = B.FILTER(1)
                        [c] = [a][country]
                """;

        ResultCollector data = executeWithErrors(dsl, true);
        data.verify("A", "a", 1, 2, 3, 4, 5);
        data.verify("A", "b", 6, 7, 8, 9, 10);
        Assertions.assertEquals(data.getValues().size(), 2);
        Assertions.assertTrue(data.getErrors().isEmpty(), "There are compilation errors");
    }

    @Test
    void testDuplicatedField() {
        String dsl = """
                table A
                    dim [a] = RANGE(5)
                    [b] = [a] + 5
                    [b] = 10
                    [b] = "TEXT"
                    [c] = [a] + [b]
                """;

        ResultCollector data = executeWithErrors(dsl, true);
        data.verify("A", "a", 1, 2, 3, 4, 5);
        data.verify("A", "b", 6, 7, 8, 9, 10);
        data.verify("A", "c", 7, 9, 11, 13, 15);
        Assertions.assertTrue(data.getErrors().isEmpty(), "There are compilation errors");
    }

    @Test
    void testContainsFunction() {
        String dsl = """
                table A
                    dim [a] = RANGE(5)
                        [b] = "234"
                        [c] = CONTAINS([b], TEXT([a]))
                        [d] = CONTAINS()
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "c", 0, 1, 1, 1, 0);

        data.verifyError("A", "d",
                "Function CONTAINS expects 2 arguments - \"text\" and \"value\", but 0 were provided");
    }

    @Test
    void testLeftFunction() {
        String dsl = """
                table A
                    dim [a] = RANGE(6)
                        [b] = "test"
                        [c] = LEFT([b], [a] - 1)
                        [d] = LEFT([b], NA)
                        [e] = LEFT([b], [b])
                        [f] = LEFT()
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "c", "", "t", "te", "tes", "test", "test");
        data.verify("A", "d", Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA,
                Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA);

        data.verify("A", "e", Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA,
                Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA);
        data.verifyError("A", "f", "Function LEFT expects 2 arguments - \"text\" and \"size\", but 0 were provided");
    }

    @Test
    void testRightFunction() {
        String dsl = """
                table A
                    dim [a] = RANGE(6)
                        [b] = "test"
                        [c] = RIGHT([b], [a] - 1)
                        [d] = RIGHT([b], NA)
                        [e] = RIGHT([b], [b])
                        [f] = RIGHT()
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "c", "", "t", "st", "est", "test", "test");
        data.verify("A", "d", Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA,
                Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA);

        data.verify("A", "e", Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA,
                Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA);
        data.verifyError("A", "f", "Function RIGHT expects 2 arguments - \"text\" and \"size\", but 0 were provided");
    }

    @Test
    void testMidFunction() {
        String dsl = """
                table A
                    dim [a] = RANGE(6)
                        [x] = [a] - 1
                        [b] = "test"
                        [c] = MID([b], [x], [a])
                        [d] = MID([b], [x], 3)
                        [e] = MID([b], NA, [a])
                        [f] = MID([b], [b], [b])
                        [g] = MID()
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "c", Strings.ERROR_NA, "te", "est", "st", "t", "");
        data.verify("A", "d", Strings.ERROR_NA, "tes", "est", "st", "t", "");
        data.verify("A", "e", Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA,
                Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA);

        data.verifyError("A", "g",
                "Function MID expects 3 arguments - \"text\", \"start\" and \"size\", but 0 were provided");
    }

    @Test
    void testSubstitute() {
        String dsl = """
                table A
                    dim [a] = RANGE(2)
                        [x] = "this is a test"
                        [y] = SUBSTITUTE(TEXT([a] + 10), TEXT([a]), "@")
                        [b] = SUBSTITUTE([x], "test", "text")
                        [g] = SUBSTITUTE()
                """;

        ResultCollector data = executeWithErrors(dsl);
        data.verify("A", "y", "@@", "1@");
        data.verify("A", "b", "this is a text", "this is a text");

        data.verifyError("A", "g",
                "Function SUBSTITUTE expects 3 arguments - \"text\", \"old\" and \"new\", but 0 were provided");
    }

    @Test
    void testMode() {
        String dsl = """
                table A
                    dim [a] = RANGE(7)
                        [b] = [a] MOD 3
                        [c] = TEXT([a])
                        [d] = TEXT([b])
                        [e] = MODE(A[a])
                        [f] = MODE(A[b])
                        [g] = MODE(A[c])
                        [h] = MODE(A[d])
                        
                table B
                    dim [a] = RANGE(5)
                        [rows] = A.FILTER($[a] > [a])
                        [b] = MODE([rows][b])
                        [c] = MODE([rows][d])
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "e", Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA,
                Doubles.ERROR_NA, Doubles.ERROR_NA);
        data.verify("A", "f", 1, 1, 1, 1, 1, 1, 1);
        data.verify("A", "g", (String) null, null, null, null, null, null, null);
        data.verify("A", "h", "1", "1", "1", "1", "1", "1", "1");

        data.verify("B", "b", 2, 0, 1, Doubles.ERROR_NA, Doubles.ERROR_NA);
        data.verify("B", "c", "2", "0", "1", null, null);
    }

    @Test
    void testCorrelation() {
        String dsl = """
                table A
                    dim [source] = RANGE(7)
                        [a] = [source] - 1
                        [b] = [a] + 3
                        [c] = -[a]
                        [d] = [a] * 1.5
                        [e] = CORREL(A[a], A[b])
                        [f] = CORREL(A[a], A[c])
                        [g] = CORREL(A[a], A[d])
                    override
                    row, [d]
                    1, 3
                    2, 1
                    3, 7
                    4, 5
                    5, -3
                    6, 9
                    7, -4
                        
                table B
                    dim [source] = RANGE(8)
                        [a] = [source] - 1
                        [rows] = A.FILTER($[a] > [a])
                        [b] = CORREL([rows][a], [rows][b])
                        [c] = CORREL([rows][a], [rows][c])
                        [d] = CORREL([rows][a], [rows][d])
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "e", 1, 1, 1, 1, 1, 1, 1);
        data.verify("A", "f", -1, -1, -1, -1, -1, -1, -1);
        data.verify("A", "g", -0.2364624979197869, -0.2364624979197869, -0.2364624979197869,
                -0.2364624979197869, -0.2364624979197869, -0.2364624979197869, -0.2364624979197869);

        data.verify("B", "b", 1.0000000000000009, 0.9999999999999999, 1.0,
                0.999999999999992, 1.0, Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA);
        data.verify("B", "c", -0.9999999999999999, -0.9999999999999999,
                -1.0, -0.9999999999999999, -1.0, Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA);
        data.verify("B", "d", -0.26939454265398016, -0.4797016118001234, -0.3077935056255462,
                -0.06911635163761368, -1.0, Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testNestedRange() {
        String dsl = """
                table A
                    dim [a] = RANGE(4)
                    dim [b] = RANGE([a] - 1)
                        [c] = RANGE([b] + 1).SUM()
                        [e] = A.FILTER(RANGE([a])).COUNT()
                """;

        ResultCollector data = executeWithErrors(dsl);

        data.verify("A", "a", 2, 3, 3, 4, 4, 4);
        data.verify("A", "b", 1, 1, 2, 1, 2, 3);
        data.verify("A", "c", 3, 3, 6, 3, 6, 10);

        data.verifyError("A", "e", "The arguments 'table_or_array' and 'condition' of the FILTER function are from different origins and may have different sizes.");
    }

    @Test
    void testPi() {
        String dsl = """
                table A
                    dim [a] = RANGE(3)
                        [b] = [a] + PI()
                        [c] = PI(1)
                """;

        ResultCollector data = executeWithErrors(dsl);

        data.verify("A", "b", PI + 1, PI + 2, PI + 3);

        data.verifyError("A", "c", "Function PI does not accept any arguments, but 1 were provided");
    }

    @Test
    void testLen() {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                    [b] = LEN([a])
                    [c] = LEN(1)
                override
                [a]
                "aaa"
                "bb"
                "c"
                """;

        ResultCollector data = executeWithErrors(dsl);

        data.verify("A", "b", 3, 2, 1);
        data.verify("A", "c", 1, 1, 1);
    }

    @ParameterizedTest
    @MethodSource("doubleUnaryFunctions")
    void testDoubleUnaryFunctions(String name, String[] values, double[] expected, double[] expectedNaNs) {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                    [b] = %1$s([a])
                override
                [a]
                %2$s
                
                table B
                    dim [x] = RANGE(3)
                    [y]
                    [z] = %1$s([y])
                override
                  row, [y]
                  1, "-Infinity"
                  2, NA
                  3, "Infinity"
                """
                .formatted(name, String.join("\n", values));

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "b", expected);
        collector.verify("B", "z", expectedNaNs);
    }

    private static Stream<Arguments> doubleUnaryFunctions() {
        return Stream.of(
                Arguments.of(
                        "ABS",
                        new String[] {"-1", "-0", "0", "1"},
                        new double[] {1, 0, 0, 1},
                        new double[] {POSITIVE_INFINITY, Doubles.ERROR_NA, POSITIVE_INFINITY}),
                Arguments.of(
                        "ACOS",
                        new String[] {"0", "1"},
                        new double[] {PI / 2, 0},
                        new double[] {Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA}),
                Arguments.of(
                        "ASIN",
                        new String[] {"0", "1"},
                        new double[] {0, PI / 2},
                        new double[] {Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA}),
                Arguments.of(
                        "ATAN",
                        new String[] {"0", "1"},
                        new double[] {0, PI / 4},
                        new double[] {-PI / 2, Doubles.ERROR_NA, PI / 2}),
                Arguments.of(
                        "CEIL",
                        new String[] {"-0.9", "-0", "0", "0.9"},
                        new double[] {-0, 0, 0, 1},
                        new double[] {NEGATIVE_INFINITY, Doubles.ERROR_NA, POSITIVE_INFINITY}),
                Arguments.of(
                        "COS",
                        new String[] {"0", String.valueOf(PI)},
                        new double[] {1, -1},
                        new double[] {Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA}),
                Arguments.of(
                        "EXP",
                        new String[] {"-1", "-0", "0", "1"},
                        new double[] {0.36787944117144233, 1.0, 1.0, 2.718281828459045},
                        new double[] {0, Doubles.ERROR_NA, POSITIVE_INFINITY}),
                Arguments.of(
                        "FLOOR",
                        new String[] {"-0.9", "-0", "0", "0.9"},
                        new double[] {-1, 0, 0, 0},
                        new double[] {NEGATIVE_INFINITY, Doubles.ERROR_NA, POSITIVE_INFINITY}),
                Arguments.of(
                        "LN",
                        new String[] {"-1", "-0", "2.718281828459045"},
                        new double[] {Doubles.ERROR_NA, Doubles.ERROR_NA, 1},
                        new double[] {Doubles.ERROR_NA, Doubles.ERROR_NA, POSITIVE_INFINITY}),
                Arguments.of(
                        "LOG10",
                        new String[] {"-1", "-0", "10"},
                        new double[] {Doubles.ERROR_NA, Doubles.ERROR_NA, 1},
                        new double[] {Doubles.ERROR_NA, Doubles.ERROR_NA, POSITIVE_INFINITY}),
                Arguments.of(
                        "ROUND",
                        new String[] {"-0.9", "-0", "0", "0.5", "0.9"},
                        new double[] {-1, 0, 0, 1, 1},
                        new double[] {NEGATIVE_INFINITY, Doubles.ERROR_NA, POSITIVE_INFINITY}),
                Arguments.of(
                        "SIN",
                        new String[] {"0", String.valueOf(PI / 2)},
                        new double[] {0, 1},
                        new double[] {Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA}),
                Arguments.of(
                        "SQRT",
                        new String[] {"-1", "-0", "0", "0.25", "1"},
                        new double[] {Doubles.ERROR_NA, -0.0, 0.0, 0.5, 1.0},
                        new double[] {Doubles.ERROR_NA, Doubles.ERROR_NA, POSITIVE_INFINITY}),
                Arguments.of(
                        "TAN",
                        new String[] {"0", String.valueOf(PI / 4)},
                        new double[] {0, 0.9999999999999999},
                        new double[] {Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA}));
    }

    @ParameterizedTest
    @MethodSource("stringUnaryFunctions")
    void testStringUnaryFunctions(String name, String[] values, String[] expected, String argument) {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                    [b] = %1$s([a])
                    [c] = %1$s(1, 2)
                override
                [a]
                %2$s
                """
                .formatted(name, String.join("\n",
                        Arrays.stream(values).map(s -> s.equals("NA") ? s : "\"" + s + "\"").toArray(String[]::new)));

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "b", expected);
        collector.verifyError("A", "c", "Function %s expects 1 argument - \"%s\", but 2 were provided"
                .formatted(name, argument));
    }

    private static Stream<Arguments> stringUnaryFunctions() {
        return Stream.of(
                Arguments.of(
                        "UNICHAR",
                        new String[] {"65", "10", "-1", "65536", "NA"},
                        new String[] {"A", "\n", Strings.ERROR_NA, Strings.ERROR_NA, Strings.ERROR_NA},
                        "code"),
                Arguments.of(
                        "LOWER",
                        new String[] {"one", "TWO", "Three", "NA"},
                        new String[] {"one", "two", "three", null},
                        "text"),
                Arguments.of(
                        "UPPER",
                        new String[] {"one", "TWO", "Three", "NA"},
                        new String[] {"ONE", "TWO", "THREE", null},
                        "text"),
                Arguments.of(
                        "TRIM",
                        new String[] {" one ", " TWO ", " Three ", "NA"},
                        new String[] {"one", "TWO", "Three", null},
                        "text"));
    }

    @Test
    void testSimpleSplit() {
        String dsl = """
                table A
                    dim [a] = SPLIT("text1--text2-text3", "-")
                        [b] = SPLIT("text1--text2-text4", "-")
                """;

        ResultCollector data = executeWithoutErrors(dsl);

        data.verify("A", "a", "text1", "", "text2", "text3");
        data.verify("A", "b", "4", "4", "4", "4");
    }

    @Test
    void testNestedSplit() {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                    [b] = NA
                override
                [a],[b]
                "text1-text2-text3","-"
                "text4||text5","|"

                table B
                    dim [x] = A
                    dim [y] = SPLIT([x][a], [x][b])
                        [z] = SPLIT([x][a], [y])
                """;

        ResultCollector data = executeWithoutErrors(dsl);

        data.verify("B", "x", 1, 1, 1, 2, 2, 2);
        data.verify("B", "y", "text1", "text2", "text3", "text4", "", "text5");
        data.verify("B", "z", "2", "2", "2", "2", "1", "2");
    }

    @Test
    void testNestedSplitWithDifferentDimensions() {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                override
                [a]
                "text1-text2-text3"
                "text4|text5"

                table B
                    dim [x] = A
                    dim [y] = SPLIT([x][a], "-")
                """;

        ResultCollector data = executeWithoutErrors(dsl);

        data.verify("B", "x", 1, 1, 1, 2);
        data.verify("B", "y", "text1", "text2", "text3", "text4|text5");
    }

    @ParameterizedTest
    @MethodSource("stringBinaryFunctions")
    void testStringBinaryFunctions(String name, String[][] values, String[] expected) {
        String dsl = """
                !manual()
                table A
                    [a] = NA
                    [b] = NA
                    [c] = %1$s([a], [b])
                    [d] = %1$s()
                override
                [a],[b]
                %2$s
                """
                .formatted(name, String.join("\n", Arrays.stream(values)
                        .map(v -> String.join(",",
                                Arrays.stream(v).map(s -> s.equals("NA") ? s : "\"" + s + "\"").toArray(String[]::new)))
                        .toList()));

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "c", expected);
        collector.verifyError("A", "d",
                "Function %s expects 2 arguments - \"text\" and \"text_to_remove\", but 0 were provided"
                        .formatted(name));
    }

    private static Stream<Arguments> stringBinaryFunctions() {
        return Stream.of(
                Arguments.of(
                        "STRIP",
                        new String[][] {{"text1", "-"}, {"-text2-", "-"}, {"--text3--", "-"}, {"---", "-"},
                                {"NA", "NA"}},
                        new String[] {"text1", "text2", "text3", "", null}),
                Arguments.of(
                        "STRIP_END",
                        new String[][] {{"text1", "-"}, {"-text2-", "-"}, {"text3--", "-"}, {"NA", "NA"}},
                        new String[] {"text1", "-text2", "text3", null}),
                Arguments.of(
                        "STRIP_START",
                        new String[][] {{"text1", "-"}, {"-text2-", "-"}, {"--text3", "-"}, {"NA", "NA"}},
                        new String[] {"text1", "text2-", "text3", null}));
    }

    @Test
    void testBinaryOperatorTypeInference() {
        String dsl = """
                table A
                    [int] = 1
                    [bool] = [int] = [int]
                    [double] = 0.5
                    [date] = DATE(2020, 1, 1)
                    [int-operations] = [int] + [bool] - [int] * [int] MOD [int]
                    [int-to-double] = [int] / [int]
                    [int-to-double2] = [int] + [double]
                    [date-operations] = POW([date] + [int] * [double], [bool])
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "int", "1");
        collector.verify("A", "bool", "TRUE");
        collector.verify("A", "double", "0.5");
        collector.verify("A", "date", "1/1/2020 12:00:00 AM");
        collector.verify("A", "int-operations", "2");
        collector.verify("A", "int-to-double", "1.0");
        collector.verify("A", "int-to-double2", "1.5");
        collector.verify("A", "date-operations", "1/1/2020 12:00:00 PM");
    }

    @Test
    void testUnaryOperatorTypeInference() {
        String dsl = """
                table A
                    [int] = 1
                    [bool] = [int] = [int]
                    [double] = 0.5
                    [date] = DATE(2020, 1, 1)
                    [neg-int] = -[int]
                    [neg-bool] = -[bool]
                    [neg-double] = -[double]
                    [neg-date] = -[date]
                    [not-int] = NOT [int]
                    [not-bool] = NOT [bool]
                    [not-double] = NOT [double]
                    [not-date] = NOT [date]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "int", "1");
        collector.verify("A", "bool", "TRUE");
        collector.verify("A", "double", "0.5");
        collector.verify("A", "date", "1/1/2020 12:00:00 AM");
        collector.verify("A", "neg-int", "-1");
        collector.verify("A", "neg-bool", "-1");
        collector.verify("A", "neg-double", "-0.5");
        collector.verify("A", "neg-date", Strings.ERROR_NA);
        collector.verify("A", "not-int", "FALSE");
        collector.verify("A", "not-bool", "FALSE");
        collector.verify("A", "not-double", "FALSE");
        collector.verify("A", "not-date", "FALSE");
    }

    @ParameterizedTest
    @MethodSource("aggregationFunctions")
    void testAggregationTypeInference(String function, Map<String, String> expectedValues) {
        String dsl = """
                table A
                    dim [source1] = RANGE(3)
                    dim [source2] = RANGE([source1] - 1)
                        [int] = [source2] - 1
                        [bool] = [int] = 1
                        [double] = [int] + 0.5
                        [date] = DATE(2020, 1, 1) + [int]

                table B
                    %s
                """.formatted(expectedValues.keySet().stream()
                .map(k -> "[" + k + "] = " + function.formatted(k))
                .collect(Collectors.joining("\n")));

        ResultCollector collector = executeWithoutErrors(dsl);

        expectedValues.forEach((name, value) -> collector.verify("B", name, value));
    }

    private static Stream<Arguments> aggregationFunctions() {
        return Stream.of(
                Arguments.of(
                        "COUNT(A[%s])",
                        Map.of(
                                "int", "3",
                                "bool", "3",
                                "double", "3",
                                "date", "3")),
                Arguments.of(
                        "COUNT(%s)",
                        Map.of(
                                "A", "3")),
                Arguments.of(
                        "SUM(A[%s])",
                        Map.of(
                                "int", "1",
                                "bool", "1",
                                "double", "2.5",
                                "date", "131494.0")),
                Arguments.of(
                        "AVERAGE(A[%s])",
                        Map.of(
                                "int", "0.3333333333333333",
                                "bool", "0.3333333333333333",
                                "double", "0.8333333333333334",
                                "date", "43831.333333333336")),
                Arguments.of(
                        "MIN(A[%s])",
                        Map.of(
                                "int", "0",
                                "bool", "FALSE",
                                "double", "0.5",
                                "date", "1/1/2020 12:00:00 AM")),
                Arguments.of(
                        "MAX(A[%s])",
                        Map.of(
                                "int", "1",
                                "bool", "TRUE",
                                "double", "1.5",
                                "date", "1/2/2020 12:00:00 AM")),
                Arguments.of(
                        "STDEVS(A[%s])",
                        Map.of(
                                "int", "0.5773502691896257",
                                "bool", "0.5773502691896257",
                                "double", "0.5773502691896257",
                                "date", "0.5773502691896257")),
                Arguments.of(
                        "STDEVP(A[%s])",
                        Map.of(
                                "int", "0.4714045207910317",
                                "bool", "0.4714045207910317",
                                "double", "0.4714045207910317",
                                "date", "0.4714045207910317")),
                Arguments.of(
                        "GEOMEAN(A[%s])",
                        Map.of(
                                "int", "0.0",
                                "bool", "0.0",
                                "double", "0.7211247851537042",
                                "date", "43831.33333079836")),
                Arguments.of(
                        "MEDIAN(A[%s])",
                        Map.of(
                                "int", "0.0",
                                "bool", "0.0",
                                "double", "0.5",
                                "date", "43831.0")),
                Arguments.of(
                        "MODE(A[%s])",
                        Map.of(
                                "int", "0",
                                "bool", "FALSE",
                                "double", "0.5",
                                "date", "1/1/2020 12:00:00 AM")),
                Arguments.of(
                        "CORREL(A[%1$s], A[%1$s])",
                        Map.of(
                                "int", "1.0",
                                "bool", "1.0",
                                "double", "0.9999999999999999",
                                "date", "0.9999999999999999")),
                Arguments.of(
                        "FIRST(A[%s])",
                        Map.of(
                                "int", "0",
                                "bool", "FALSE",
                                "double", "0.5",
                                "date", "1/1/2020 12:00:00 AM")),
                Arguments.of(
                        "FIRST(%s)",
                        Map.of(
                                "A", "1")),
                Arguments.of(
                        "LAST(A[%s])",
                        Map.of(
                                "int", "1",
                                "bool", "TRUE",
                                "double", "1.5",
                                "date", "1/2/2020 12:00:00 AM")),
                Arguments.of(
                        "LAST(%s)",
                        Map.of(
                                "A", "3")),
                Arguments.of(
                        "SINGLE(FILTER(A, $[source1] = 2)[%s])",
                        Map.of(
                                "int", "0",
                                "bool", "FALSE",
                                "double", "0.5",
                                "date", "1/1/2020 12:00:00 AM")),
                Arguments.of(
                        "SINGLE(FILTER(%s, $[source1] = 2))",
                        Map.of(
                                "A", "1")),
                Arguments.of(
                        "INDEX(A[%s], 1)",
                        Map.of(
                                "int", "0",
                                "bool", "FALSE",
                                "double", "0.5",
                                "date", "1/1/2020 12:00:00 AM")),
                Arguments.of(
                        "INDEX(%s, 1)",
                        Map.of(
                                "A", "1")));
    }

    @Test
    void testAutoCastForConcat() {
        String dsl = """
                table A
                    [a] = 1 & "12"
                    [b] = "12" & (0 = 0)
                    [c] = "" & DATE(2024, 4, 5)
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "a", "112");
        collector.verify("A", "b", "12TRUE");
        collector.verify("A", "c", "45387.0");
    }

    @Test
    void testAutoCastToDouble() {
        String dsl = """
                table A
                    [a] = "12" + "14"
                    [b] = "5" * 6.3
                    [c] = 1 / "2"
                    [d] = ABS("-5")
                    [e] = POW("2", "5")
                    [f] = DATE("1999", "11", "30")
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "a", "26.0");
        collector.verify("A", "b", "31.5");
        collector.verify("A", "c", "0.5");
        collector.verify("A", "d", "5.0");
        collector.verify("A", "e", "32.0");
        collector.verify("A", "f", "11/30/1999 12:00:00 AM");
    }

    @Test
    void testDateRange() {
        String dsl = """
                table SECOND
                    dim [a] = DATERANGE(DATE(2023, 01, 01), DATE(2023, 01, 01) + 1/(24*60*6), 1, 1)
                table MINUTE
                    dim [a] = DATERANGE(DATE(2023, 01, 01) + 1/1000000, DATE(2023, 01, 01) + 6/(24*60), 1, 2)
                table HOUR
                    dim [a] = DATERANGE(DATE(2023, 01, 01) + 1/1000000, DATE(2023, 01, 01) + 5/24, 1, 3)
                table DAY
                    dim [a] = DATERANGE(DATE(2023, 01, 01) + 1/1000000, DATE(2023, 01, 05))
                table WORKDAY
                    dim [a] = DATERANGE(DATE(2024, 04, 01) + 1/1000000, DATE(2024, 04, 10), 1, 5)
                table WORKDAY_INCREMENT_2
                    dim [a] = DATERANGE(DATE(2024, 04, 01) + 1/1000000, DATE(2024, 04, 10), 2, 5)
                table WEEK
                    dim [a] = DATERANGE(DATE(2024, 04, 01) + 1/1000000, DATE(2024, 05, 10), 1, 6)
                table MONTH
                    dim [a] = DATERANGE(DATE(2024, 01, 01) + 1/1000000, DATE(2024, 05, 10), 1, 7)
                table QUARTER
                    dim [a] = DATERANGE(DATE(2024, 02, 01) + 1/1000000, DATE(2025, 05, 10), 1, 8)
                table YEAR
                    dim [a] = DATERANGE(DATE(2024, 01, 01) + 1/1000000, DATE(2026, 05, 10), 1, 9)
                table VALIDATION1
                    dim [a] = DATERANGE(DATE(2024, 05, 01), DATE(2024, 01, 10), 1, 9)
                table VALIDATION2
                    dim [a] = DATERANGE(DATE(2024, 05, 01), DATE(2026, 01, 10), 0, 9)
                table VALIDATION3
                    dim [a] = DATERANGE(DATE(2024, 05, 01), DATE(2026, 01, 10), 1, 10)
                table NA_TEST
                    dim [a] = DATERANGE(DATE(2024, 05, 01), NA, 1, 10)
                table NESTED
                    dim [type] = RANGE(2)
                    [dates] = DATERANGE(DATE(2024, 01, 01), DATE(2024, 01, 02), 1, [type] + 2)
                """;
        ResultCollector data = executeWithErrors(dsl);

        data.verify("SECOND", "a", "1/1/2023 12:00:00 AM", "1/1/2023 12:00:01 AM", "1/1/2023 12:00:02 AM",
                "1/1/2023 12:00:03 AM", "1/1/2023 12:00:04 AM", "1/1/2023 12:00:05 AM", "1/1/2023 12:00:06 AM",
                "1/1/2023 12:00:07 AM", "1/1/2023 12:00:08 AM", "1/1/2023 12:00:09 AM", "1/1/2023 12:00:10 AM");
        data.verify("MINUTE", "a", "1/1/2023 12:01:00 AM", "1/1/2023 12:02:00 AM", "1/1/2023 12:03:00 AM",
                "1/1/2023 12:04:00 AM", "1/1/2023 12:05:00 AM", "1/1/2023 12:06:00 AM");
        data.verify("HOUR", "a", "1/1/2023 01:00:00 AM", "1/1/2023 02:00:00 AM", "1/1/2023 03:00:00 AM",
                "1/1/2023 04:00:00 AM", "1/1/2023 05:00:00 AM");
        data.verify("DAY", "a", "1/2/2023 12:00:00 AM", "1/3/2023 12:00:00 AM", "1/4/2023 12:00:00 AM",
                "1/5/2023 12:00:00 AM");
        data.verify("WORKDAY", "a", "4/2/2024 12:00:00 AM", "4/3/2024 12:00:00 AM", "4/4/2024 12:00:00 AM",
                "4/5/2024 12:00:00 AM", "4/8/2024 12:00:00 AM", "4/9/2024 12:00:00 AM", "4/10/2024 12:00:00 AM");
        data.verify("WORKDAY_INCREMENT_2", "a", "4/2/2024 12:00:00 AM", "4/4/2024 12:00:00 AM", "4/8/2024 12:00:00 AM",
                "4/10/2024 12:00:00 AM");
        data.verify("WEEK", "a", "4/7/2024 12:00:00 AM", "4/14/2024 12:00:00 AM", "4/21/2024 12:00:00 AM",
                "4/28/2024 12:00:00 AM", "5/5/2024 12:00:00 AM");
        data.verify("MONTH", "a", "2/1/2024 12:00:00 AM", "3/1/2024 12:00:00 AM", "4/1/2024 12:00:00 AM",
                "5/1/2024 12:00:00 AM");
        data.verify("QUARTER", "a", "4/1/2024 12:00:00 AM", "7/1/2024 12:00:00 AM", "10/1/2024 12:00:00 AM",
                "1/1/2025 12:00:00 AM", "4/1/2025 12:00:00 AM");
        data.verify("YEAR", "a", "1/1/2025 12:00:00 AM", "1/1/2026 12:00:00 AM");
        data.verifyError("VALIDATION1", "a",
                "Invalid argument \"date1\" or \"date2\" for function DATERANGE: expected \"date1\" to be greater or equal to \"date2\".");
        data.verifyError("VALIDATION2", "a",
                "Invalid argument \"increment\" for function DATERANGE: expected a positive number.");
        data.verifyError("VALIDATION3", "a",
                "Invalid argument \"date_type\" for function DATERANGE: expected a number from 1 to 9.");
        data.verify("NA_TEST", "a");
        data.verify("NESTED", "dates", "25", "2");
    }

    @Test
    void testWorkDay() {
        String dsl = """
                    table A
                        [a] = WORKDAY(DATE(2024, 04, 11), 2)
                        [b] = WORKDAY(DATE(2024, 04, 11), 1)
                        [c] = WORKDAY(DATE(2024, 04, 11), 0)
                        [d] = WORKDAY(DATE(2024, 04, 11), -1)
                        [e] = WORKDAY(DATE(2024, 04, 11), -2)
                        [f] = WORKDAY(DATE(2024, 04, 11), -3)
                        [g] = WORKDAY(DATE(2024, 04, 11), -4)
                        [h] = WORKDAY(DATE(2024, 04, 11), -NA)
                        [i] = WORKDAY(DATE(2024, 04, 11) + 1/100000, 0)
                    table B
                        dim [a] = RANGE(12)
                        [date] = WORKDAY(DATE(2024, 04, 11), [a] - 6)
                """;

        ResultCollector data = executeWithErrors(dsl);

        data.verify("A", "a", "4/15/2024 12:00:00 AM");
        data.verify("A", "b", "4/12/2024 12:00:00 AM");
        data.verify("A", "c", "4/11/2024 12:00:00 AM");
        data.verify("A", "d", "4/10/2024 12:00:00 AM");
        data.verify("A", "e", "4/9/2024 12:00:00 AM");
        data.verify("A", "f", "4/8/2024 12:00:00 AM");
        data.verify("A", "g", "4/5/2024 12:00:00 AM");
        data.verify("A", "h", Doubles.ERROR_NA);
        data.verify("A", "i", "4/11/2024 12:00:00 AM");
        data.verify("B", "date", "4/4/2024 12:00:00 AM", "4/5/2024 12:00:00 AM", "4/8/2024 12:00:00 AM",
                "4/9/2024 12:00:00 AM", "4/10/2024 12:00:00 AM", "4/11/2024 12:00:00 AM", "4/12/2024 12:00:00 AM",
                "4/15/2024 12:00:00 AM", "4/16/2024 12:00:00 AM", "4/17/2024 12:00:00 AM", "4/18/2024 12:00:00 AM",
                "4/19/2024 12:00:00 AM");
    }

    @Test
    void testList() {
        String dsl = """
                table A
                  dim [a] = {1, "2"}
                  dim [b] = {5 - 2}
                      [c] = {}
                      [e1] = {[a]}
                      [e2] = {A}
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "a", "1", "2");
        collector.verify("A", "b", 3, 3);
        collector.verify("A", "c", "0", "0");

        collector.verifyError("A", "e1", "LIST function accepts texts or numbers only.");
        collector.verifyError("A", "e2", "Invalid argument \"element\" for function LIST: expected a text or a number, but got a table.");
    }

    @Test
    void testPythonExpression() {
        String dsl = """
                table A
                    dim [x] = RANGE(5)
                    [y] = [x] + 10
                    [z] = PYSUM([x], [y])
                    [n] = PYNAN()
                    [i] = PYINF()
                    [none] = PYNONE()
                    [cat] = PYCAT()
                    [escape] = PYESCAPE()
                    [same] = PYSAME([escape])
                    [f1] = A.FILTER(PYSUM($[x], [x]) > 3).COUNT()
                    [f2] = A.FILTER(PYSUM([x], [x]) > 3).COUNT()
                    [f3] = A.FILTER(PYSUM($[x], $[x]) > 3).COUNT()
                               
                ```python
                                
                import math
                                
                def pysum(arg1: float, arg2: float) -> float:
                  return arg1 + arg2
                                
                def pynan() -> float:
                  return math.nan
                                
                def pyinf() -> float:
                  return math.inf
                                
                def pynone() -> float:
                  return None
                                
                def pycat():
                  return "cat"
                                
                def pyescape():
                  return "\\n\\r\\\\"
                                
                def pysame(arg):
                  return arg
                                
                ```
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        double inf = POSITIVE_INFINITY;

        collector.verify("A", "x", 1, 2, 3, 4, 5);
        collector.verify("A", "y", 11, 12, 13, 14, 15);
        collector.verify("A", "z", 12, 14, 16, 18, 20);
        collector.verify("A", "n", Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA,
                Doubles.ERROR_NA);
        collector.verify("A", "i", inf, inf, inf, inf, inf);
        collector.verify("A", "none", Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA,
                Doubles.ERROR_NA);
        collector.verify("A", "cat", "cat", "cat", "cat", "cat", "cat");
        collector.verify("A", "escape", "\n\r\\", "\n\r\\", "\n\r\\", "\n\r\\", "\n\r\\");
        collector.verify("A", "same", "\n\r\\", "\n\r\\", "\n\r\\", "\n\r\\", "\n\r\\");
        collector.verify("A", "f1", "3", "4", "5", "5", "5");
        collector.verify("A", "f2", "0", "5", "5", "5", "5");
        collector.verify("A", "f3", "4", "4", "4", "4", "4");
    }

    @Test
    void testPythonPlan() {
        String dsl = """
                table A
                    dim [x] = RANGE(5)
                    [y] = [x] + 10
                    [a] = A.FILTER($[x] < 2)[x]
                    [b] = A.FILTER($[y] - 9 > [x])[x]
                    [w] = PYX([a], [b], [x])
                                
                table B
                    dim [x] = PYCONSTANTS()
                    dim [y] = PYLIST([x])
                        [z] = PYFILTER(A.FILTER([x] = $[x])[x], 2).COUNT()
                                
                ```python
                def pyx(arg1: list[float], arg2: list[float], arg3: float) -> float:
                  return len(arg1) + len(arg2) + arg3
                                
                def pyconstants() -> list[float]:
                  return [1, 2, 4]
                                
                def pylist(size: float) -> list[str]:
                  list = []
                  for i in range(0, int(size)):
                    list.append(2 * i)
                  return list
                               
                def pyfilter(l : list[float], x : float) -> list[float]:
                    result = []
                    for i in l:
                      if i != x:
                         result.append(i)
                    return result
                ```
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "x", 1, 2, 3, 4, 5);
        collector.verify("A", "y", 11, 12, 13, 14, 15);
        collector.verify("A", "a", 1, 1, 1, 1, 1);
        collector.verify("A", "b", 5, 4, 3, 2, 1);
        collector.verify("A", "w", 7, 7, 7, 7, 7);
        collector.verify("B", "x", 1, 2, 2, 4, 4, 4, 4);
        collector.verify("B", "y", "0", "0", "2", "0", "2", "4", "6");
        collector.verify("B", "z", 1, 0, 0, 1, 1, 1, 1);
    }

    @Test
    void testPythonError() {
        String dsl = """
                table A
                    dim [x] = RANGE(5)
                    [w] = SUM()
                    [e] = EXCEPTION()
                    [t] = TIMEOUT()
                                
                ```python
                import time
                                
                def sum():
                  return "1";
                                
                def exception():
                  raise Exception('I am really sorry')
                                
                def timeout():
                  time.sleep(60)
                  return "1"
                ```
                """;

        ResultCollector collector = executeWithErrors(dsl);
        collector.verifyError("A", "w", "Python function has same name as function: SUM");
        collector.verifyError("A", "e", "Failed to execute python function: exception");
        collector.verifyError("A", "t", "Failed to execute python function: timeout");
    }

    @Test
    void testRowReference() {
        String dsl = """
                table A
                  dim [a] = RANGE(3)
                      [b] = A(1)
                      [c] = 'A'(2)
                      [e1] = 'RANGE'(2)
                      [e2] = A(2, 3)
                                
                table B
                  key dim [a] = RANGE(4)
                          [b] = A(3)[a]
                          [c] = A([a] + 1)[a]
                          [e] = B(1, "2")
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("A", "b", 1, 1, 1);
        collector.verify("A", "c", 2, 2, 2);
        collector.verifyError("A", "e1", "Unknown table: RANGE");
        collector.verifyError("A", "e2", "RowReference must have 1 argument for a table without keys");

        collector.verify("B", "b", 3, 3, 3, 3);
        collector.verify("B", "c", 2, 3, Doubles.ERROR_NA, Doubles.ERROR_NA);
        collector.verifyError("B", "e", "RowReference must have 1 arguments, but got: 2");
    }

    @Test
    void testVectorMathSimple() {
        String dsl = """
                table T1
                  dim [a] = RANGE(5)
                  [b] = [a] ^ 2
                  
                table T2
                  dim [a] = RANGE(5) + 1
                  [d] = POW(2, ROW())
                  
                table T3
                  dim [b] = T1[a] + T1[a]
                  [f] = FIRST(T1, [b])
                  [x] = SUM([f][b] + [f][a])
                  [y] = T1[a] + T2[a]
                  [z] = ROW() + [f][a]
                  [zs] = SUM([z])
                  [w] = IFNA(12 / IF(T2[a] - 2, T2[a] - 2, NA), 0).INDEX(ROW())
                  [v] = LOG(T2[d], 2).INDEX(ROW())
                  [u] = (CONCAT(T2[a], [b])).INDEX(ROW())
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("T2", "a", 2, 3, 4, 5, 6);
        collector.verify("T2", "d", 2, 4, 8, 16, 32);
        collector.verify("T3", "b", 2, 4, 6, 8, 10);
        collector.verify("T3", "x", 8, 40, 70, 70, 70);
        collector.verify("T3", "y", 5, 5, 5, 5, 5);
        collector.verify("T3", "z", 2, 4, 5, 5, 5);
        collector.verify("T3", "zs", 5, 18, 30, 35, 40);
        collector.verify("T3", "w", 0, 12, 6, 4, 3);
        collector.verify("T3", "v", 1, 2, 3, 4, 5);
        collector.verify("T3", "u", "22", "34", "46", "58", "610");
    }

    @Test
    void testTwoDimensionVectorMath() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)

                table B
                  dim [b] = RANGE(2)
                  dim [c] = RANGE(3)
                  [x] = A.FILTER($[a] <= [b] + [c])[a]
                  [y] = [b] + [x]
                  [z] = SUM([y])
                """;


        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("B", "b", 1, 1, 1, 2, 2, 2);
        collector.verify("B", "c", 1, 2, 3, 1, 2, 3);
        collector.verify("B", "x", 2, 3, 4, 3, 4, 5);
        collector.verify("B", "y", 2, 3, 4, 3, 4, 5);
        collector.verify("B", "z", 5, 9, 14, 12, 18, 25);
    }


    @Test
    void testThreeDimensionVectorMath() {
        String dsl = """
                table B
                  dim [x] = RANGE(4)
                                
                table A
                  dim [a] = RANGE(2) + 2000
                  dim [b] = RANGE(3)
                  dim [c] = DATE([a], [b], B[x])
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        double date_2001_1_1 = 36892.0;
        double date_2002_1_1 = 36892.0 + 365;
        double jan = 31.0;
        double feb = 31 + 28.0; // 2001 and 2002 are not leap years
        collector.verify("A", "c",
                date_2001_1_1, date_2001_1_1 + 1, date_2001_1_1 + 2, date_2001_1_1 + 3,
                date_2001_1_1 + jan, date_2001_1_1 + jan + 1, date_2001_1_1 + jan + 2, date_2001_1_1 + jan + 3,
                date_2001_1_1 + feb, date_2001_1_1 + feb + 1, date_2001_1_1 + feb + 2, date_2001_1_1 + feb + 3,
                date_2002_1_1, date_2002_1_1 + 1, date_2002_1_1 + 2, date_2002_1_1 + 3,
                date_2002_1_1 + jan, date_2002_1_1 + jan + 1, date_2002_1_1 + jan + 2, date_2002_1_1 + jan + 3,
                date_2002_1_1 + feb, date_2002_1_1 + feb + 1, date_2002_1_1 + feb + 2, date_2002_1_1 + feb + 3);
    }

    @Test
    void testIncorrectVectorMath1() {
        String dsl = """
                table B
                  dim [x] = RANGE(4)
                  
                table A
                  dim [a] = RANGE(2)
                  [b] = A.FILTER(B[x])
                  [c] = A[a] + B[x]
                """;

        ResultCollector collector = executeWithErrors(dsl);
        collector.verifyError("A", "b", "The arguments 'table_or_array' and 'condition' of the FILTER function are from different origins and may have different sizes.");
        collector.verifyError("A", "c", "Operands of the '+' operator are from different origins and may have different sizes.");
    }

    @Test
    void testIncorrectVectorMath2() {
        String dsl = """
                table Q
                  dim [q] = RANGE(2) - 1
                  
                table C
                  dim [c] = RANGE(3) - 2
                  [f1] = Q.FILTER(COUNT([c])) # incorrect
                  [f2] = Q.FILTER(COUNT(C[c]))
                  [f2s] = Q.FILTER(SUM(C[c]))
                  [f3] = Q.FILTER(COUNT(Q[q]))
                  [f4] = Q.FILTER(COUNT($[q])) # correct as $ is substituted with Q
                  
                  [f7] = C.FILTER(COUNT([c])) # incorrect
                  [f8] = C.FILTER(COUNT(C[c]))
                  [f9] = C.FILTER(COUNT($[c]))
                """;
//              TODO: It works but adds projection which we cannot optimize
//                [f5] = Q.FILTER(SUM(Q[q] * [c])) # correct, Q[q] + [c] is nested Number.
//                [f6] = Q.FILTER(SUM($[q] * [c])) # doesn't work. Projection is missing
//                [f10] = C.FILTER(SUM(C[c] * [c])) # Projection
//                [f11] = C.FILTER(SUM($[c] * [c])) # Projection
//
        ResultCollector collector = executeWithErrors(dsl);
        collector.verifyError("C", "f1",
                "Invalid argument \"table_or_array\" for function COUNT: expected a table or an array, but got a number. Did you mean C[c]?");
        collector.verify("C", "f2", 2, 2, 2);
        collector.verify("C", "f2s", 0, 0, 0);
        collector.verify("C", "f3", 2, 2, 2);
        collector.verify("C", "f4", 2, 2, 2);
//        collector.verify("C", "f5", 1, 0, 1);
//        collector.verify("C", "f6", 1, 0, 1);

        collector.verifyError("C", "f7",
                "Invalid argument \"table_or_array\" for function COUNT: expected a table or an array, but got a number. Did you mean C[c]?");
        collector.verify("C", "f8", 3, 3, 3);
        collector.verify("C", "f9", 3, 3, 3);
//        collector.verify("C", "f10", 3, 0, 3);
//        collector.verify("C", "f11", 3, 0, 3);
    }

    @Test
    void testIncorrectVectorMath3() {
        String dsl = """
                table B
                  dim [x] = RANGE(4)
                  
                table A
                  dim [a] = RANGE(2)
                  [b] = A.FIND([a] + B[x])
                """;

        ResultCollector collector = executeWithErrors(dsl);
        collector.verifyError("A", "b",
                "Invalid argument \"keys\" for function FIND: expected a text or a number, but got an array.");
    }

    @Test
    void testExcelRefSyntax() {
        String dsl = """
                table T1
                  dim [r1] = RANGE(20)
                  
                table T2
                  dim [r2] = RANGE(5)
                  [f1] = T1.FILTER([r2] <= T1[r1])
                  [f2] = T1.FILTER(T1[r1] + [r2] < T1[r1].COUNT())
                  [f3] = T1.FILTER($[r1] + [r2] < T1.COUNT())
                  [f4] = T1.FILTER(T1[r1] + [r2] < T1[r1].MAX())
                  [f5] = T1.FILTER(T1[r1] < [r2] + T2[r2].COUNT())
                  [f6] = T1.FILTER(T1[r1] < [r2] + T2.COUNT())
                  [nf1] = [f1].FILTER(T1[r1] <= [r2])
                  [nf2] = [f1].FILTER(T1[r1] <= [r2] + T3[r3].COUNT())
                  [nf3] = [f1].FILTER(T1[r1] <= [r2] + T1[r1].COUNT())
                  
                table T3
                    dim [r3] = T2.SORTBY(-T2[r2])[r2]               
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verify("T2", "f1", 20, 19, 18, 17, 16);
        collector.verify("T2", "f2", 18, 17, 16, 15, 14);
        collector.verify("T2", "f3", 18, 17, 16, 15, 14);
        collector.verify("T2", "f4", 18, 17, 16, 15, 14);
        collector.verify("T2", "f5", 5, 6, 7, 8, 9);
        collector.verify("T2", "f6", 5, 6, 7, 8, 9);
        collector.verify("T3", "r3", 5, 4, 3, 2, 1);

        collector.verifyError("T2", "nf1", "The arguments 'table_or_array' and 'condition' of the FILTER function are from different origins and may have different sizes.");
        collector.verifyError("T2", "nf2", "The arguments 'table_or_array' and 'condition' of the FILTER function are from different origins and may have different sizes.");
        collector.verifyError("T2", "nf3", "The arguments 'table_or_array' and 'condition' of the FILTER function are from different origins and may have different sizes.");
    }

    @Test
    void testRowFunction() {
        String dsl = """
                table A
                  dim [a] = RANGE(3)
                      [b] = POW(ROW(), 2)
                      [c] = RANGE(ROW())
                                
                table B
                  dim [a] = RANGE(3)
                  dim [b] = RANGE(3)
                      [c] = POW(ROW(), 2)
                                
                table C
                  dim [a] = RANGE(2)
                      [b] = A.FILTER($[a] >= [a] * ROW())
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "a", 1, 2, 3);
        collector.verify("A", "b", 1, 4, 9);
        collector.verify("A", "c", 1, 2, 3);

        collector.verify("B", "a", 1, 1, 1, 2, 2, 2, 3, 3, 3);
        collector.verify("B", "b", 1, 2, 3, 1, 2, 3, 1, 2, 3);
        collector.verify("B", "c", 1, 4, 9, 16, 25, 36, 49, 64, 81);

        collector.verify("C", "a", 1, 2);
        collector.verify("C", "b", 3, 0);
    }

    @Test
    void testApplyDim0() {
        String dsl = """
                table A
                  [a] = 10
                  [b] = [a] + 3
                  [c] = ROW()
                apply
                  filter [a] <= 3
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a");
        collector.verify("A", "b");
        collector.verify("A", "c");
    }

    @Test
    void testApplyDim1() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                      [b] = [a] + 3
                      [c] = ROW()
                apply
                  filter [a] <= 3
                  sort -[a]
                override
                  row, [b]
                  1, 10
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", 3, 2, 1);
        collector.verify("A", "b", 10, 5, 4);
        collector.verify("A", "c", 1, 2, 3);
    }

    @Test
    void testApplyDim2() {
        String dsl = """
                table A
                  dim [a] = RANGE(3)
                  dim [b] = RANGE(4)
                      [c] = [a] + [b]
                      [d] = ROW()
                apply
                  filter [a] < 3 AND [b] < 4
                  sort [b], [a]
                override
                  row, [c]
                  1, 10
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", 1, 2, 1, 2, 1, 2);
        collector.verify("A", "b", 1, 1, 2, 2, 3, 3);
        collector.verify("A", "c", 10, 3, 3, 4, 4, 5);
    }

    @Test
    void testApplyAfterKeyOverride() {
        String dsl = """
                table A
                  key dim [a] = RANGE(5)
                      [b] = [a]
                      [c] = ROW()
                apply
                  sort -[b]
                  filter [b] <= 3
                override
                  key [a], [b]
                  1, 10
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", 3, 2);
        collector.verify("A", "b", 3, 2);
        collector.verify("A", "c", 1, 2);
    }

    @Test
    void testApplyAfterRowOverride() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                      [b] = [a]
                      [c] = ROW()
                apply
                  filter [b] <= 3
                  sort -[b]
                override
                  row, [b]
                  1, 10
                """;

        ResultCollector collector = executeWithErrors(dsl);
        String error = "Can't apply filter. Make sure you do not use overridden columns "
                + "in filter for a table without keys. Error: Cyclic dependency: A";

        collector.verifyError("A", "a", error);
        collector.verifyError("A", "b", "Cyclic dependency: A");
        collector.verifyError("A", "c", error);
    }

    @Test
    void testApplyAfterManualOverride() {
        String dsl = """
                !manual()
                table A
                  [a] = NA
                  [b] = [a] + 10
                  [c] = ROW()
                apply
                  filter [a] <= 3 AND [a] > 1
                  sort -[a]
                override
                  [a]
                  1
                  2
                  3
                  4
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", 3, 2);
        collector.verify("A", "b", 13, 12);
        collector.verify("A", "c", 1, 2);
    }

    @Test
    void testApplyIncorrectType() {
        String dsl = """
                table A
                   dim [a] = RANGE(4)
                   
                 table B
                   dim [b] = RANGE(2)
                   apply
                      filter A[a]

                 table C
                   dim [c] = RANGE(3)
                   apply
                      sort A[a]
                 """;

        ResultCollector collector = executeWithErrors(dsl);
        collector.verifyError("B", "b",
                CompileApply.INCORRECT_FILTER_LAYOUT);
        collector.verifyError("C", "c",
                CompileApply.INCORRECT_SORT_LAYOUT);
    }

    @Test
    void testValidOverrideFormulas() {
        String dsl = """
                !manual()
                table A
                  [a] = ROW()
                override
                  [a]
                  2
                                
                  "4"
                  1 + 5
                  A.COUNT()
                                
                table B
                  [b] = NA
                override
                  row, [b]
                  1, FIRST(A[a])
                  2, LAST(A[a])
                                
                table C
                  key dim [x] = RANGE(5)
                      [c] = 10
                override
                  [x], [c]
                  0, 5 + 1
                  3, C(1)[x]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", "2", "2", "4", "6", "5");
        collector.verify("B", "b", "2");
        collector.verify("C", "c", 10, 10, 1, 10, 10);
    }

    @Test
    void testTotals() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                      [b] = [a] & " word"
                      [c] = A.TOTAL(2)
                      [a_sum] = A.TOTAL()[a]
                      [a_count] = A.TOTAL(2)[a]
                      [b_first] =  A.TOTAL(1)[b]
                      [b_last] = [c][b]
                total
                      [a] = SUM(A[a])
                      [b] = FIRST(A[b])
                total
                      [a] = COUNT(A[a])
                      [b] = LAST(A[b])
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", 1, 2, 3, 4, 5);
        collector.verify("A", "b", "1 word", "2 word", "3 word", "4 word", "5 word");
        collector.verify("A", "c", 1, 1, 1, 1, 1);
        collector.verify("A", "a_sum", 15, 15, 15, 15, 15);
        collector.verify("A", "a_count", 5, 5, 5, 5, 5);
        collector.verify("A", "b_first", "1 word", "1 word", "1 word", "1 word", "1 word");
        collector.verify("A", "b_last", "5 word", "5 word", "5 word", "5 word", "5 word");

        collector.verifyTotal("A", "a", 1, 15);
        collector.verifyTotal("A", "b", 1, "1 word");
        collector.verifyTotal("A", "a", 2, 5);
        collector.verifyTotal("A", "b", 2, "5 word");
    }

    @Test
    void testInvalidTotals() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                      [b] = [a] & " word"
                      [c] = A.TOTAL(2)[a]
                      [d] = A.TOTAL(0)[a]
                      [e] = A.TOTAL(1)[d]
                total
                      [a] = SUM([a])
                      [a] = SUM(A[a])
                      [b] = A
                """;

        ResultCollector collector = executeWithErrors(dsl, true);
        collector.verifyError("A", "c", "Table: A has only 1 total definitions");
        collector.verifyError("A", "d", "TOTAL function requires positive number in 2 argument");
        collector.verifyError("A", "e", "Unknown total. Table: A. Column: d. Number: 1");

        collector.verifyTotalError("A", "a", 1, "Cannot access current row's [a] outside of column formula. Try A[a]?");
        collector.verifyTotalError("A", "b", 1, "expected a text or a number, but got a table.");
    }

    @Test
    void testTableWithNonUniqueKeys() {
        String dsl = """
                table A
                  dim [x] = RANGE(2)
                  key [y] = 1
                      [z] = A(1)[x]
                """;

        ResultCollector collector = executeWithErrors(dsl);
        collector.verifyError("A", "z", "Table contains non unique keys");
    }

    @Test
    void testTableFunctionSpecialCases() {
        String dsl = """
                table A
                   dim [a] = RANGE(2)
                   [f1] = A[a].FILTER(A[a] - [a]).SUM()
                   [b1] = [f1][a].SUM()
                   [f2] = A.UNIQUEBY(1)
                   [b2] = [f2][a].SUM()
                   [f3] = A.SORTBY(1)
                   [b3] = [f3][a].MAX()
                """;

        ResultCollector collector = executeWithErrors(dsl);
        // TODO: Not supported yet. Hard to implement as A[a] is promoted as cart on data, not row ref + projection.
        //collector.verify("A", "f1", 1, 1);
        //collector.verify("A", "b1", 2, 1);
        collector.verify("A", "f2", 1, 1);
        collector.verify("A", "b2", 1, 1);
        collector.verify("A", "f3", 2, 2);
        collector.verify("A", "b3", 2, 2);
    }

    @Test
    void testFilterInFilter() {
        String dsl = """
                table B
                  dim [a] = RANGE(4)

                table A
                dim [r] = RANGE(5)
                    [b1] = B.FILTER([r]).COUNT() # all 4's
                    # same but inline [b1] formula
                    [c1] = A.FILTER(A[r] < B.FILTER([r]).COUNT())
                    [c2] = A.FILTER([r] = B.FILTER([r]).COUNT())
                    [b3] = B.FILTER([r] = B[a])[a].LAST()
                    # same but inline [b2] formula
                    [c3] = A.FILTER(A[r] = B.FILTER([r] = B[a])[a].SINGLE())[r].SINGLE()
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "b1", 4, 4, 4, 4, 4);
        collector.verify("A", "c1", 3, 3, 3, 3, 3);
        collector.verify("A", "c2", 0, 0, 0, 5, 0);
        collector.verify("A", "b3", 1, 2, 3, 4, Doubles.ERROR_NA);
        collector.verify("A", "c3", 1, 2, 3, 4, Doubles.ERROR_NA);
    }

    @Test
    void test5x3Pivot() {
        String dsl = """
                table T1
                  dim [r] = RANGE(5)
                  [*] = PIVOT(RANGE(3), $, FIRST($))
                  
                table T2
                  dim [r] = RANGE(5)
                  [*] = PIVOT(RANGE(3), TEXT($), FIRST($))
                  
                table T3
                  dim [r] = RANGE(5)
                  [*] = PIVOT(RANGE(3), VALUE($), FIRST($))
                  
                table T1f
                  dim [f] = FIELDS(T1).SORT()
                   
                table T2f
                  dim [f] = FIELDS(T2).SORT()
                   
                table T3f
                  dim [f] = FIELDS(T3).SORT()
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("T1f", "f", "1", "2", "3", "r");
        collector.verify("T2f", "f", "1", "2", "3", "r");
        collector.verify("T3f", "f", "1.0", "2.0", "3.0", "r");
    }

    @Test
    void testRecursiveOverride() {
        String dsl = """
                table A
                  dim [a] = RANGE(4)
                      [b] = A(5)[a]
                override
                  row, [a]
                  1, 10
                  2, A(1)[a] + 10
                  3, A(2)[a] + A(1)[a]
                  4, A(3)[a] + A(2)[a]
                  5, 42
                                
                table B
                  key dim [a] = RANGE(10).FILTER(4 <= $ AND $ <= 7)
                          [b] = [a] + 10
                          [c] = [a] + 20
                override
                  [a], [b], [c]
                  1, B(0)[b],
                  4, B(2)[c], B(3)[b]
                  5, B(6)[c], B(6)[b]
                  6, B(7)[c], B(10)[c]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", 10, 20, 30, 50);
        collector.verify("A", "b", Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA);

        collector.verify("B", "a", 4, 5, 6, 7);
        collector.verify("B", "b", Doubles.ERROR_NA, Doubles.ERROR_NA, 27, 17);
        collector.verify("B", "c", Doubles.ERROR_NA, 27, Doubles.ERROR_NA, 27);
    }

    @Test
    void testSameRowOverride() {
        String dsl = """
                table A
                  dim [a] = RANGE(7)
                      [b] = [a] + 10
                      [c] = A[a].FILTER([a] < $)
                override
                  row, [a], [b]
                  2, 100, [a]
                  3, , [a]
                  4, , A.FILTER([a] < $[a]).COUNT()
                  5, , ROW()
                  6, , [c].COUNT()
                  7, , ROW() + A[a].FILTER([a] - ROW() < $).COUNT()
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", 1, 100, 3, 4, 5, 6, 7);
        collector.verify("A", "b", 11, 100, 3, 4, 5, 2, 14);
    }

    @Test
    void testPivotOverride() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                      [*] = A.FILTER([a] <= $[a]).PIVOT("p_" & $[a], COUNT($[a]))
                      [b] = [p_3]
                      [c] = [p_4]
                override
                  row, [p_3]
                  3, [p_4] + 9
                  4, A(1)[p_1] + 4
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "b", 1, 1, 10, 5, Doubles.EMPTY);
        collector.verify("A", "c", 1, 1, 1, 1, Doubles.EMPTY);
    }

    @Test
    void testOverridesInManualTableWithKeys() {
        String dsl = """
                !manual()
                table A
                  key [a] = NA
                  [b] = NA
                  [c] = NA
                  [d] = NA
                  [e] = NA
                override
                  [a], [b], [c], [d], [e]
                  1, 2, 3, 0, 0
                  4, A(4)[a], [a], A(1)[d], ROW()
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "a", 1, 4);
        collector.verify("A", "b", 2, 4);
        collector.verify("A", "c", 3, 4);
        collector.verify("A", "d", 0, 0);
        collector.verify("A", "e", 0, 2);
    }

    @Test
    void testOverridesInManualTableWithApply() {
        String dsl = """
                 !manual()
                 table A
                   [a] = NA
                   [b] = NA
                   [c] = NA
                   [d] = NA
                   [e] = NA
                 apply
                   sort -[a]
                 override
                   [a], [b], [c], [d], [e]
                   1, 2, 3, 0, 0
                   4, A(1)[a], [a], A(1)[d], ROW()
                """;

        ResultCollector collector = executeWithErrors(dsl, false, true);
        collector.verify("A", "a", 4, 1);
        collector.verify("A", "b", 4, 2);

        collector.verifyOverrideError("A", "c", 2,
                "Not allowed to use current columns or use ROW() in overrides for manual table with keys or apply section");
        collector.verifyOverrideError("A", "d", 2, "Cyclic dependency: A[d]");
        collector.verifyOverrideError("A", "e", 2,
                "Not allowed to use current columns or use ROW() in overrides for manual table with keys or apply section");
    }

    @Test
    void testOverridesKeyAutoCast() {
        String dsl = """
                   table A
                     key dim [a] = RANGE(5)
                     [b] = NA
                   override
                   [a], [b]
                   "2", 10
                   "3.0", A("2")[b] + 1
                   "4.00", A("3.00")[b] + 1
                                
                   table B
                     key dim [a] = TEXT(RANGE(5))
                     [b] = NA
                   override
                   [a], [b]
                   2, 10
                   3.0, B("2")[b] + 1
                   4.00, B("3")[b] + 1
                                
                   !manual()
                   table C
                     key [a] = NA
                     [b] = NA
                   override
                   [a], [b]
                   1,
                   2, 10
                   3.0, C("2.00")[b] + 1
                   4.00, C("3.000")[b] + 1
                   5,
                                
                   !manual()
                   table D
                     key [a] = -1
                     [b] = NA
                   override
                   [a], [b]
                   1,
                   2, 10
                   3.0, D(2)[b] + 1
                   "4", D(3)[b] + 1
                   5,
                                
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "a", 1, 2, 3, 4, 5);
        collector.verify("A", "b", Doubles.ERROR_NA, 10, 11, 12, Doubles.ERROR_NA);

        collector.verify("B", "a", "1", "2", "3", "4", "5");
        collector.verify("B", "b", Doubles.ERROR_NA, 10, 11, 12, Doubles.ERROR_NA);

        collector.verify("C", "a", 1, 2, 3, 4, 5);
        collector.verify("C", "b", Doubles.ERROR_NA, 10, 11, 12, Doubles.ERROR_NA);

        collector.verify("D", "a", "1", "2", "3", "4", "5");
        collector.verify("D", "b", Doubles.ERROR_NA, 10, 11, 12, Doubles.ERROR_NA);
    }

    @Test
    void testMod() {
        String dsl = """
                !manual()
                table A
                  [a]
                  [b]
                  [MOD] = [a] MOD [b]
                override
                  [a], [b]
                  3, 4
                  3, -4
                  -3, 4
                  -3, -4
                  3, 0
                  0, 3
                  -3, 0
                  0, -3
                  -0, -3
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        double NA = Doubles.ERROR_NA;
        collector.verify("A", "MOD", 3, -1, 1, -3, NA, 0, NA, 0, 0);
    }

    @Test
    void testMathFunctionsWithSpecialValues() {
        String dsl = """
                !manual()
                table A
                  [b]
                  [NEG]   = -[b]
                  [ABS]   = ABS([b])
                  [SQRT]  = SQRT([b])
                  [ROUND] = ROUND([b])
                  [FLOOR] = FLOOR([b])
                  [CEIL]  = CEIL([b])
                  [EXP]   = EXP([b])
                  [LN]    = LN([b])
                  [LOG10] = LOG10([b])
                  [SIN]   = SIN([b])
                  [COS]   = COS([b])
                  [TAN]   = TAN([b])
                  [ASIN]  = ASIN([b])
                  [ACOS]  = ACOS([b])
                  [ATAN]  = ATAN([b])
                override
                  [b]
                  ""
                  NA
                
                !manual()
                table B
                  [b]
                  [c]
                  [ADD]   = [b] + [c]
                  [SUB]   = [b] - [c]
                  [MUL]   = [b] * [c]
                  [DIV]   = [b] / [c]
                  [MOD]   = [b] MOD [c]
                  [LOG]   = LOG([b], [c])
                  [POW]   = POW([b], [c])
                override
                  [b], [c]
                  "", 1
                  NA, 1
                  1, ""
                  1, NA
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        String NA = Strings.ERROR_NA;

        collector.verify("A", "NEG", "0.0", NA);
        collector.verify("A", "ABS", "0.0", NA);
        collector.verify("A", "SQRT", "0.0", NA);
        collector.verify("A", "ROUND", "0", NA);
        collector.verify("A", "FLOOR", "0", NA);
        collector.verify("A", "CEIL", "0", NA);
        collector.verify("A", "EXP", "1.0", NA);
        collector.verify("A", "LN", NA, NA);
        collector.verify("A", "LOG10", NA, NA);
        collector.verify("A", "SIN", "0.0", NA);
        collector.verify("A", "COS", "1.0", NA);
        collector.verify("A", "TAN", "0.0", NA);
        collector.verify("A", "ASIN", "0.0", NA);
        collector.verify("A", "ACOS", "1.5707963267948966", NA);
        collector.verify("A", "ATAN", "0.0", NA);

        collector.verify("B", "ADD", "1.0", NA, "1.0", NA);
        collector.verify("B", "SUB", "-1.0", NA, "1.0", NA);
        collector.verify("B", "MUL", "0.0", NA, "0.0", NA);
        collector.verify("B", "DIV", "0.0", NA, NA, NA);
        collector.verify("B", "MOD", "0.0", NA, NA, NA);
        collector.verify("B", "LOG", NA, NA, NA, NA);
        collector.verify("B", "POW", "0.0", NA, "1.0", NA);
    }

    @Test
    void testTextFunctionsWithSpecialValues() {
        String dsl = """
                !manual()
                table A
                  [b]
                  [VALUE] = VALUE([b])
                  [LOWER] = LOWER([b])
                  [UPPER] = UPPER([b])
                  [LEN]   = LEN([b])
                  [TRIM]  = TRIM([b])
                  [UNICHAR] = UNICHAR([b])
                  [TEXT] = TEXT([b])
                override
                  [b]
                  ""
                  NA
                                
                !manual()
                table B
                  [b]
                  [c]
                  [CONCAT] = CONCAT([b], [c])
                  [LEFT]  = LEFT([b], [c])
                  [RIGHT] = RIGHT([b], [c])
                  [CONTAINS]= CONTAINS([b], [c])
                  [STRIP] = STRIP([b], [c])
                  [STRIP_START]= STRIP_START([b], [c])
                  [STRIP_END]= STRIP_END([b], [c])
                override
                  [b], [c]
                  "", 1
                  NA, 1
                  1, ""
                  1, NA
                                
                !manual()
                table C
                  [b]
                  [c]
                  [d]
                  [MID]   = MID([b], [c], [d])
                  [SUBSTITUTE] = SUBSTITUTE([b], [c], [d])
                override
                  [b], [c], [d]
                  "", 1, 1
                  NA, 1, 1
                  1, "", 1
                  1, NA, 1
                  1, 1, ""
                  1, 1, NA
                """;

        String NA = Strings.ERROR_NA;
        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "VALUE", "", NA);
        collector.verify("A", "LOWER", "", NA);
        collector.verify("A", "UPPER", "", NA);
        collector.verify("A", "LEN", "0", NA);
        collector.verify("A", "TRIM", "", NA);
        collector.verify("A", "UNICHAR", NA, NA);
        collector.verify("A", "TEXT", "", NA);

        collector.verify("B", "b", "", NA, "1", "1");
        collector.verify("B", "c", "1", "1", "", NA);

        collector.verify("B", "CONCAT", "1", NA, "1", NA);
        collector.verify("B", "LEFT", "", NA, "", NA);
        collector.verify("B", "RIGHT", "", NA, "", NA);
        collector.verify("B", "CONTAINS", "FALSE", NA, "TRUE", NA);
        collector.verify("B", "STRIP", "", NA, "1", NA);
        collector.verify("B", "STRIP_START", "", NA, "1", NA);
        collector.verify("B", "STRIP_END", "", NA, "1", NA);

        collector.verify("C", "MID", "", NA, NA, NA, "", NA);
        collector.verify("C", "SUBSTITUTE", "", NA, "1", NA, "", NA);
    }

    @Test
    void testDateFunctionsWithSpecialValues() {
        String dsl = """
                !manual()
                table A
                  [b]
                  [YEAR]   = YEAR([b])
                  [MONTH]  = MONTH([b])
                  [DAY]    = DAY([b])
                  [HOUR]   = HOUR([b])
                  [MINUTE] = MINUTE([b])
                  [SECOND] = SECOND([b])
                override
                  [b]
                  -1
                  0.11
                
                  NA
                  1.88
                  1000.44
                
                !manual()
                table B
                  [b]
                  [c]
                  [WORKDAY] = WORKDAY([b], [c])
                override
                  [b], [c]
                  , 1
                  NA, 1
                  1,
                  1, NA
                  ,
                  0.1,
                  , 0.1
                  0, -1
                
                !manual()
                table C
                  [b]
                  [c]
                  [d]
                  [DATE]   = DATE([b], [c], [d])
                override
                  [b], [c], [d]
                  , 12, 30
                  NA, 1, 1
                  1900, , 1
                  1900, NA, 1
                  1900, 1,
                  1900, 1, NA
                  1900, ,
                  1900, 0, 0
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "YEAR", Doubles.ERROR_NA, 1899, 1899, Doubles.ERROR_NA, 1899, 1902);
        collector.verify("A", "MONTH", Doubles.ERROR_NA, 12, 12, Doubles.ERROR_NA, 12, 9);
        collector.verify("A", "DAY", Doubles.ERROR_NA, 30, 30, Doubles.ERROR_NA, 31, 26);
        collector.verify("A", "HOUR", Doubles.ERROR_NA, 2, 0, Doubles.ERROR_NA, 21, 10);
        collector.verify("A", "MINUTE", Doubles.ERROR_NA, 38, 0, Doubles.ERROR_NA, 7, 33);
        collector.verify("A", "SECOND", Doubles.ERROR_NA, 24, 0, Doubles.ERROR_NA, 12, 36);

        collector.verify("B", "WORKDAY", 2, Doubles.ERROR_NA, 1,
                Doubles.ERROR_NA, 0, 0, 0, Doubles.ERROR_NA);

        collector.verify("C", "DATE", 0, Doubles.ERROR_NA, Doubles.ERROR_NA,
                Doubles.ERROR_NA, 1, Doubles.ERROR_NA, Doubles.ERROR_NA, Doubles.ERROR_NA);
    }

    @Test
    void testLogicalFunctionsWithSpecialValues() {
        String dsl = """
                !manual()
                table A
                  [b]
                  [ISNA] = ISNA([b])
                  [NOT] = NOT [b]
                override
                  [b]
                
                  NA
                
                !manual()
                table B
                  [b]
                  [c]
                  [IFNA] = IFNA([b], [c])
                  [AND]  = [b] AND [c]
                  [OR]   = [b] OR [c]
                  [LT]   = [b] < [c]
                  [GT]   = [b] > [c]
                  [LTE]  = [b] <= [c]
                  [GTE]  = [b] >= [c]
                  [NEQ]  = [b] <> [c]
                  [EQ]   = [b] = [c]
                override
                  [b], [c]
                  , 1
                  NA, 1
                  1,
                  1, NA
                  ,
                
                !manual()
                table C
                  [b]
                  [c]
                  [d]
                  [IF]   = IF([b], [c], [d])
                override
                  [b], [c], [d]
                  , 1, 2
                  NA, 1, 2
                  1, , 2
                  1, NA, 2
                  1, 2,
                  1, 2, NA
                  0, NA, 1
                  -0, NA, 1
                """;

        String NA = Strings.ERROR_NA;
        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "ISNA", "FALSE", "TRUE");
        collector.verify("A", "NOT", "FALSE", NA);

        collector.verify("B", "b", "", NA, "1.0", "1.0", "");
        collector.verify("B", "c", "1.0", "1.0", "", NA, "");
        collector.verify("B", "IFNA", "", "1.0", "1.0", "1.0", "");
        collector.verify("B", "AND", "FALSE", NA, "FALSE", NA, "FALSE");
        collector.verify("B", "OR", "TRUE", NA, "TRUE", NA, "FALSE");
        collector.verify("B", "LT", "FALSE", NA, "FALSE", NA, "FALSE");
        collector.verify("B", "GT", "FALSE", NA, "FALSE", NA, "FALSE");
        collector.verify("B", "LTE", "FALSE", NA, "FALSE", NA, "TRUE");
        collector.verify("B", "GTE", "FALSE", NA, "FALSE", NA, "TRUE");
        collector.verify("B", "NEQ", "TRUE", NA, "TRUE", NA, "FALSE");
        collector.verify("B", "EQ", "FALSE", NA, "FALSE", NA, "TRUE");

        collector.verify("C", "IF", "2.0", NA, "", NA, "2.0", "2.0", "1.0", "1.0");
    }

    @Test
    void testAggregationsWithSpecialValues() {
        String dsl = """
                !manual()
                table A
                  [a] = ROW()
                  [b]
                  [c] = A.FILTER($[a] <> [a])[b]
                  [COUNT]   = COUNT([c])
                  [SUM]     = SUM([c])
                  [AVERAGE] = AVERAGE([c])
                  [MAX]     = MAX([c])
                  [MIN]     = MIN([c])
                  [STDEVS]  = STDEVS([c])
                  [STDEVP]  = STDEVP([c])
                  [GEOMEAN] = GEOMEAN([c])
                  [MEDIAN]  = MEDIAN([c])
                  [MODE]    = MODE([c])
                  [FIRST]   = FIRST([c])
                  [LAST]    = LAST([c])
                  [SINGLE]  = SINGLE([c])
                override
                  [b]
                  ""
                  NA
                  1
                  2
                
                !manual()
                table B
                  [a] = ROW()
                  [b]
                  [c]
                  [d] = B.FILTER([a] <> $[a])
                  # [FIRST]   = [d].FIRST($[b]) does not support errors
                  # [LAST]    = [d].LAST($[b]) does not support errors
                  [MAXBY]   = [d].MAXBY($[b])
                  [MINBY]   = [d].MINBY($[b])
                  [CORREL]  = CORREL([d][b], [d][c])
                override
                  [b], [c]
                  "", 1
                  NA, 2
                  1, ""
                  2, NA
                  3, 4
                """;

        String NA = Strings.ERROR_NA;
        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "c", "3", "3", "3", "3");
        collector.verify("A", "COUNT", "3", "2", "2", "2");
        collector.verify("A", "SUM", NA, "3.0", NA, NA);
        collector.verify("A", "AVERAGE", NA, "1.5", NA, NA);
        collector.verify("A", "MAX", NA, "2.0", NA, NA);
        collector.verify("A", "MIN", NA, "1.0", NA, NA);
        collector.verify("A", "STDEVS", NA, "0.7071067811865476", NA, NA);
        collector.verify("A", "STDEVP", NA, "0.5", NA, NA);
        collector.verify("A", "GEOMEAN",NA, "1.414213562373095", NA, NA);
        collector.verify("A", "MEDIAN", NA, "1.5", NA, NA);
        collector.verify("A", "MODE", NA, NA, NA, NA);
        collector.verify("A", "FIRST", NA, "", "", "");
        collector.verify("A", "LAST", "2", "2", "2", "1");
        collector.verify("A", "SINGLE", NA, NA, NA, NA);

        collector.verify("B", "MAXBY", "5", "5", "5", "5", "4");
        collector.verify("B", "MINBY", "3", "3", "4", "3", "3");
        collector.verify("B", "CORREL", NA, NA, NA, NA, NA);
    }

    @Test
    void testTableFunctionsWithSpecialValues() {
        String dsl = """
                !manual()
                table Z
                  [a]
                override
                  [a]
                  0
               
                  NA
                  1
              
                table A
                  key dim [a] = Z[a]
                  [FILTER]   = A.FILTER([a] = $[a])
                  [FIND]     = A.FIND([a])
               
               !manual()
                table B
                  [a]
                override
                  [a]
                  0
                  -0
               
                  NA
                  1
               
                  NA
                  1
               
                table C
                  dim [UNIQUE] = UNIQUE(VALUE(B[a]))
               
                table D
                  dim [SORT] = SORT(TEXT(B[a]))
               
                table E
                  dim [SORT] = SORT(B[a])
               """;

        ResultCollector collector = executeWithoutErrors(dsl);
        String NA = Strings.ERROR_NA;

        collector.verify("A", "FILTER", "1", "1", "0", "1");
        collector.verify("A", "FIND", "1", "2", NA, "4");

        collector.verify("C", "UNIQUE", "0.0", "", NA, "1.0");
        collector.verify("D", "SORT",  "", "", "-0.0", "0.0", "1.0", "1.0", NA, NA);
        collector.verify("E", "SORT",   "0.0", "-0.0", "1.0", "1.0", "", "", NA, NA);
    }

    @Test
    void testEmptyValuesInOverrides() {
        String dsl = """
                table A
                  key dim [x] = RANGE(5)
                  key [y]
                  [z]
                override
                [x],[y],[z]
                3,"",10
                4,"",[x] + [y]
                5,"",A(3, "")[z]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "z", "", "", "10.0", "4.0", "10.0");
    }

    @Test
    void testCastingWithLogicalBinaryOperators() {
        String dsl = """
                table A
                  [b] = 10
                  [c] = TEXT([b])
                  [AND]  = [b] AND [c]
                  [OR]   = [b] OR [c]
                  [LT]   = [b] < [c]
                  [GT]   = [b] > [c]
                  [LTE]  = [b] <= [c]
                  [GTE]  = [b] >= [c]
                  [NEQ]  = [b] <> [c]
                  [EQ]   = [b] = [c]
                
                table B
                  [b] = TEXT([c])
                  [c] = 10
                  [AND]  = [b] AND [c]
                  [OR]   = [b] OR [c]
                  [LT]   = [b] < [c]
                  [GT]   = [b] > [c]
                  [LTE]  = [b] <= [c]
                  [GTE]  = [b] >= [c]
                  [NEQ]  = [b] <> [c]
                  [EQ]   = [b] = [c]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("B", "AND", "TRUE");
        collector.verify("B", "OR", "TRUE");
        collector.verify("B", "LT", "FALSE");
        collector.verify("B", "GT", "FALSE");
        collector.verify("B", "LTE", "TRUE");
        collector.verify("B", "GTE", "TRUE");
        collector.verify("B", "NEQ", "FALSE");
        collector.verify("B", "EQ", "TRUE");
    }

    @Test
    void testCarryThroughExpandedPeriodSeries() {
        String dsl = """
                 table Table1
                   dim [source] = RANGE(3)
                   [ps] = PERIODSERIES(Table1[source] + 1, Table1[source], "DAY")
                
                 table Table2
                   dim [source] = Table1[ps]
                   dim [v] = [source]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("Table2", "v",
                "(1900-01-01, 1.0)", "(1900-01-02, 2.0)", "(1900-01-03, 3.0)",
                "(1900-01-01, 1.0)", "(1900-01-02, 2.0)", "(1900-01-03, 3.0)",
                "(1900-01-01, 1.0)", "(1900-01-02, 2.0)", "(1900-01-03, 3.0)");
    }

    @Test
    void testSelfReferenceInRange() {
        String dsl = """
                 table A
                   dim [a] = RANGE(3)
                   [b] = RANGE([a] + A.COUNT()) # 1,2,3 + 3 = 4,5,6
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("A", "b", "4", "5", "6");
    }

    @Test
    void testEvaluation() {
        String dsl = """
                !manual()
                table Ingredients
                  !description("price_type")
                  [item] = NA
                  [price_type]
                  [rus_item]
                override
                [item],[price_type],[rus_item]
                "Chicken","cheap","Курица"
                "Beef","expensive","Говядина"
                "Pork","cheap","Свинина"
                "Carrots","expensive","Морковь"
                "Potatoes","expensive","Картошка"
                "Tomatoes","cheap","Томаты"
                "Onions","expensive","Лук"
                "Bell peppers","cheap","Перец"
                "Spinach","expensive","Шпинат"
                "Zucchini","expensive","Цуккини"
                "Garlic","cheap","Чеснок"
                "Broccoli","expensive","Брокколи"
                "Mushrooms","cheap","Грибы"
                "Cabbage","cheap","Капуста"
                "Eggplant","cheap","Баклажан"
                "Cauliflower","cheap","Цветная капуста"
                "Green beans","cheap","Зеленая фасоль"
                "Asparagus","expensive","Спаржа"
                "Kale","expensive","Капуста"
                "Sweet potatoes","expensive","Сладкий картофель"

                !evaluation()
                !manual()
                table Evaluation
                  [Passed] = [Avg Recall] = 1
                  [Retrieve Rus Items] = \\
                    Ingredients[rus_item].RETRIEVE( \\
                        [Question], \\
                        EVALUATE_N(Ingredients[rus_item]), \\
                        EVALUATE_MODEL(Ingredients[rus_item]) \\
                    )
                  [Retrieve Rus Items Scores] = \\
                    Ingredients[rus_item].RETRIEVE_SCORES( \\
                        [Question], \\
                        EVALUATE_N(Ingredients[rus_item]), \\
                        EVALUATE_MODEL(Ingredients[rus_item]) \\
                    )
                  [Avg Recall] = ( \\
                      RECALL( \\
                          Ingredients[item].RETRIEVE( \\
                              [Question], \\
                              EVALUATE_N(Ingredients[item]), \\
                              EVALUATE_MODEL(Ingredients[item]) \\
                          ), \\
                          [Ground truth for items] \\
                      ) + \\
                      RECALL( \\
                          Ingredients[price_type].RETRIEVE( \\
                              [Question], \\
                              EVALUATE_N(Ingredients[price_type]), \\
                              EVALUATE_MODEL(Ingredients[price_type]) \\
                          ), \\
                          [Ground truth for types] \\
                      ) + \\
                      RECALL( \\
                          Ingredients[rus_item].RETRIEVE( \\
                              [Question], \\
                              EVALUATE_N(Ingredients[rus_item]), \\
                              EVALUATE_MODEL(Ingredients[rus_item]) \\
                          ), \\
                          [Ground truth for rus items] \\
                      ) \\
                    ) / 3
                    !evaluation_question()
                    [Question] = NA
                    !evaluation_field("Ingredients", "item")
                    [Ground truth for items]
                    !evaluation_field("Ingredients", "price_type")
                    [Ground truth for types]
                    !evaluation_field("Ingredients", "rus_item")
                    [Ground truth for rus items]
                override
                [Question],[Ground truth for items],[Ground truth for types],[Ground truth for rus items]
                "What cheap vegetables do I have?","Carrots;Potatoes;Onions;Garlic","cheap",
                "Onion?","Onions;Potatoes","cheap",
                "капуста",,,"Цветная капуста;Капуста"

                table EvaluatedModels
                  [Item model] = EVALUATE_MODEL(Ingredients[item])
                  [Type model] = EVALUATE_MODEL(Ingredients[price_type])
                  [Rus Item model] = EVALUATE_MODEL(Ingredients[rus_item])
                  [Item N] = EVALUATE_N(Ingredients[item])
                  [Type N] = EVALUATE_N(Ingredients[price_type])
                  [Rus Item N] = EVALUATE_N(Ingredients[rus_item])

                table EvaluationResult
                    [Number of passed] = Evaluation[Passed].FILTER($ = 1).COUNT()
                    [Avg recall] = Evaluation[Avg Recall].AVERAGE()
                    
                table Recall
                    [recall] = RECALL({"Onions", "Test"}, "Onions;Test;Test2")
                                                                                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("EvaluationResult", "Number of passed", "3");
        collector.verify("EvaluationResult", "Avg recall", "1.0");

        collector.verify("EvaluatedModels", "Item model", "bge-small-en-v1.5");
        collector.verify("EvaluatedModels", "Type model", "bge-small-en-v1.5");
        collector.verify("EvaluatedModels", "Rus Item model", "multilingual-e5-small");
        collector.verify("EvaluatedModels", "Item N", "16");
        collector.verify("EvaluatedModels", "Type N", "1");
        collector.verify("EvaluatedModels", "Rus Item N", "2");

        collector.verify("Evaluation", "Passed", 1, 1, 1);
        collector.verify("Evaluation", "Retrieve Rus Items", "2", "2", "2");
        collector.verify("Evaluation", "Retrieve Rus Items Scores", "2", "2", "2");
        collector.verify("Evaluation", "Avg Recall", 1, 1, 1);

        collector.verify("Recall", "recall", 0.6666666666666666);
    }

    @Test
    void testAssemblingConditionAtOptimizingFilter() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                  [b] = [a] - 1
                
                table B
                  dim [x] = RANGE(4)
                  [y] = A.FILTER([x] = $[a] AND $[a] > 0 AND $[a] < 0)
                """;

        ResultCollector collector = executeWithoutErrors(dsl);
        collector.verify("B", "y", "0", "0", "0", "0");
    }

    @Test
    void testBetween() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                  [b] = BETWEEN([a], 2, 4)
                  [c] = BETWEEN(TEXT([a]), 2, 4)
                  [d] = BETWEEN([a] & "a", "2a", "4a")
                
                table B
                  [x] = BETWEEN(NA, 2, 4)
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "b", "FALSE", "TRUE", "TRUE", "TRUE", "FALSE");
        collector.verify("A", "c", "FALSE", "TRUE", "TRUE", "TRUE", "FALSE");
        collector.verify("A", "d", "FALSE", "TRUE", "TRUE", "TRUE", "FALSE");
        collector.verify("B", "x", Doubles.ERROR_NA);
    }

    @ParameterizedTest
    @MethodSource("setOperations")
    void testSetOperations(
            String name,
            double[] simpleDouble,
            String[] simpleString,
            double[] nestedRows,
            double[] nestedValues) {
        String dsl = """
                !manual()
                table A
                  [num]
                  [str]
                override
                [num],[str]
                NA,NA
                ,
                1,"a"
                10,"Z"
                
                !manual()
                table B
                  [num]
                  [str]
                override
                [num],[str]
                NA,NA
                ,
                1,"a"
                2,"z"
                
                table SimpleNumeric
                  dim [a] = A[num].%1$s(B[num])

                table SimpleString
                  dim [a] = A[str].%1$s(B[str])
                
                table Nested
                  dim [a] = RANGE(2)
                  dim [b] = A.FILTER($[num] <> [a])[num].%1$s(RANGE([a]))
                """.formatted(name);

        ResultCollector collector = executeWithoutErrors(dsl, true);

        collector.verify("SimpleNumeric", "a", simpleDouble);
        collector.verify("SimpleString", "a", simpleString);
        collector.verify("Nested", "a", nestedRows);
        collector.verify("Nested", "b", nestedValues);
    }

    private static Stream<Arguments> setOperations() {
        return Stream.of(
                Arguments.of(
                        "UNION",
                        new double[] {Doubles.ERROR_NA, Doubles.EMPTY, 1, 10, 2},
                        new String[] {Strings.ERROR_NA, Strings.EMPTY, "a", "Z", "z"},
                        new double[] {1, 1, 1, 2, 2, 2, 2},
                        new double[] {Doubles.EMPTY, 10, 1, Doubles.EMPTY, 1, 10, 2}),
                Arguments.of(
                        "INTERSECT",
                        new double[] {Doubles.ERROR_NA, Doubles.EMPTY, 1},
                        new String[] {Strings.ERROR_NA, Strings.EMPTY, "a"},
                        new double[] {2},
                        new double[] {1}),
                Arguments.of(
                        "SUBTRACT",
                        new double[] {10},
                        new String[] {"Z"},
                        new double[] {1, 1, 2, 2},
                        new double[] {Doubles.EMPTY, 10, Doubles.EMPTY, 10}));
    }

    @Test
    void testMixedDimensionsInUnion() {
        String dsl = """
                !manual()
                table A
                  [a]
                override
                [a]
                NA
                
                1
                10
                
                table DimMix
                  dim [a] = RANGE(2)
                  dim [b] = UNION(A[a] + [a], RANGE(3))
                """;

        ResultCollector collector = executeWithoutErrors(dsl, true);

        collector.verify("DimMix", "a", 1, 1, 1, 1, 1, 2, 2, 2, 2, 2);
        collector.verify("DimMix", "b", Doubles.ERROR_NA, 1, 2, 11, 3, Doubles.ERROR_NA, 2, 3, 12, 1);
    }

    @Test
    void testStringAutoCastInUnion() {
        String dsl = """
                !manual()
                table A
                  [num]
                  [str]
                override
                [num],[str]
                NA,NA
                ,
                1,"1.0"
                10,"Z"
                
                table B
                  dim [a] = UNION(A[num], A[str])
                """;

        ResultCollector collector = executeWithoutErrors(dsl, true);

        collector.verify("B", "a", Strings.ERROR_NA, Strings.EMPTY, "1.0", "10.0", "Z");
    }

    @Test
    void testSimpleIn() {
        String dsl = """
                table A
                  [a] = 1.IN({1})
                  [b] = 1.IN({2})
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "a", 1);
        collector.verify("A", "b", 0);
    }

    @Test
    void testSimpleInWithCurrent() {
        String dsl = """
                !manual()
                table A
                  [a]
                  [b]
                override
                [a],[b]
                NA,NA
                ,
                2,"a"
                2,"a"
                3,"b"
                
                table B
                  dim [a] = {NA, 0, 1, 10}
                  [b] = [a].IN(A[a] - 1)
                
                table C
                  dim [a] = {NA, "", "a", "A"}
                  [b] = [a].IN(A[b])
                
                table D
                  dim [a] = RANGE(3)
                  [b] = [a].IN({1, 2, 6} - [a])
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("B", "b", 0, 0, 1, 0);
        collector.verify("C", "b", 0, 1, 1, 0);
        collector.verify("D", "b", 1, 0, 1);
    }

    @Test
    void testNestedIn() {
        String dsl = """
                !manual()
                table A
                  [a]
                  [b]
                override
                [a],[b]
                NA,NA
                ,
                2,"a"
                2,"a"
                3,"b"
                
                table B
                  dim [a] = {NA, 0, 1, 10}.IN(A[a] - 1)
                
                table C
                  dim [a] = {NA, "", "a", "A"}.IN(A[b])
                
                table D
                  dim [a] = A.FILTER(A[a].IN({NA, 2, 4}))[a]

                table E
                  dim [a] = A.FILTER(1).FILTER($[a].IN({NA, 2, 4}))[a]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("B", "a", 0, 0, 1, 0);
        collector.verify("C", "a", 0, 1, 1, 0);
        collector.verify("D", "a", 2, 2);
        collector.verify("E", "a", 2, 2);
    }

    @Test
    void testNestedInWithCurrent() {
        String dsl = """
                table A
                  dim [a] = RANGE(3)
                
                table B
                  dim [a] = RANGE(4)
                  dim [b] = A.FILTER(1).FILTER(([a] + $[a]).IN({2, 4}))[a]
                
                table C
                  dim [a] = RANGE(4)
                  dim [b] = A.FILTER(1).FILTER([a].IN({2, 4}))
                
                table D
                  dim [a] = RANGE(4)
                  dim [b] = A.FILTER(1).FILTER($[a].IN({2, 4} - [a]))[a]
                
                table E
                  dim [a] = RANGE(4)
                  dim [b] = A.FILTER(1).FILTER([a].IN({2, 4} - [a]))[a]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("B", "a", 1, 1, 2, 3);
        collector.verify("B", "b", 1, 3, 2, 1);
        collector.verify("C", "a", 2, 2, 2, 4, 4, 4);
        collector.verify("C", "b", 1, 2, 3, 1, 2, 3);
        collector.verify("D", "a", 1, 1, 2, 3);
        collector.verify("D", "b", 1, 3, 2, 1);
        collector.verify("E", "a", 1, 1, 1, 2, 2, 2);
        collector.verify("E", "b", 1, 2, 3, 1, 2, 3);
    }

    @Test
    void testErrorColumn() {
        String dsl = """
                table A
                  dim [a] = RANGE(2)
                      [*] = PIVOT(RANGE(3), $, SUM($))
                      [b] = -[missing]
                      [c] = [missing] + 1
                      [d] = RANGE([missing])
                      [e] = IF([missing], 1, 2)
                
                table B
                  [a] = A.FILTER(1=1)[a].COUNT()
                  [b] = A.FILTER(1=1)[missing].COUNT()
                  [c] = A.FILTER($[missing]=1).COUNT()
                  [d] = A(1)[a]
                  [e] = A(1)[missing]
                  [f] = A[missing].SUM()
                
                table C
                   dim [a] = RANGE(3)
                       [b] = A.FILTER($[a] = [a])[a].COUNT()
                       [c] = A.FILTER($[a] = [a])[missing].COUNT()
                       [d] = A.FILTER($[missing] = [a]).COUNT()
                
                table D
                   dim [a] = RANGE(4)
                   dim [b] = A
                       [c] = [b][a] # carry on cartesian
                       [d] = [b][missing]
                
                table E
                   dim [a] = RANGE(5)
                   dim [b] = A[missing]
                
                table F
                   dim [a] = RANGE(6)
                       [b] = A.FIRST()[missing]
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verifyError("A", "b", "The column 'missing' does not exist in the pivot table.");
        collector.verifyError("A", "c", "The column 'missing' does not exist in the pivot table.");
        collector.verifyError("A", "d", "The column 'missing' does not exist in the pivot table.");
        collector.verify("B", "a", 2);
        collector.verifyError("B", "b", "The column 'missing' does not exist in the pivot table.");
        collector.verifyError("B", "c", "The column 'missing' does not exist in the pivot table.");
        collector.verify("B", "d", 1);
        collector.verifyError("B", "e", "The column 'missing' does not exist in the pivot table.");
        collector.verifyError("B", "f", "The column 'missing' does not exist in the pivot table.");
        collector.verify("C", "b", 1, 1, 0);
        collector.verifyError("C", "c", "The column 'missing' does not exist in the pivot table.");
        collector.verifyError("C", "d", "The column 'missing' does not exist in the pivot table.");
        collector.verify("D", "c", 1, 2, 1, 2, 1, 2, 1, 2);
        collector.verifyError("D", "d", "The column 'missing' does not exist in the pivot table.");
        collector.verifyError("E", "b", "The column 'missing' does not exist in the pivot table.");
        collector.verifyError("F", "b", "The column 'missing' does not exist in the pivot table.");
    }

    @Test
    void testVectorMathWithOneDim() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                      [b] = [a] + 1
                
                table B
                  dim [c] = RANGE(6)
                      [d] = (A[a] + [c]) + (A[b] + [c])
                      [e] = (-A[a] + [c]) + (A[b] + 5)
                      [f] = (FILTER(A, 1)[a] + [c]) + (FILTER(A, 1)[b] + [c])
                      [g] = FILTER(A, [d])
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testVectorMathWithTwoDims() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                      [b] = [a] + 1
                
                table B
                  dim [c] = RANGE(3)
                  dim [d] = RANGE(4)
                      [e] = (A[a] + [c]) + (A[b] + [d])
                      [f] = (RANGE(10) + [c]) + (-RANGE(10) + [d])
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testVectorMathWithFourDims() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                      [b] = [a] + 1
                
                table B
                  dim [c] = RANGE(2)
                  dim [d] = RANGE(3)
                  dim [e] = RANGE([d])
                  dim [f] = RANGE(4)
                      [g] = (A[a] + [c]) + (A[b] - [d]) + (A[a] + [e]) + (-A[b] + [f])
                      [h] = (RANGE(6) + [c]) + (RANGE(6) - [d]) + (RANGE(6) + [e]) + (-RANGE(6) + [f])
                """;

        executeWithoutErrors(dsl);
    }

    @Test
    void testMinusOperator() {
        String dsl = """
                table A
                  [a] = 2-1
                  [b] = 2 - -1
                  [c] = -(-5)
                  [d] = - 7
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "a", 1);
        collector.verify("A", "b", 3);
        collector.verify("A", "c", 5);
        collector.verify("A", "d", -7);
    }

    @Test
    void testPowOperator() {
        String dsl = """
                table A
                  [a] = 2 + 4 ^ 2
                  [b] = 2 * 4 ^ 2
                  [c] = 2 * 4 ^ -2
                  [d] = 2 * 4 ^ -(2)
                  [e] = 2 * 4 ^ 2 + 1
                  [f] = 2 * 4 ^ 2 * 3
                  [g] = 2 * 4 ^ 2 ^ 3  # excel pow has left associativity unlike other languages
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "a", 18);
        collector.verify("A", "b", 32);
        collector.verify("A", "c", 0.125);
        collector.verify("A", "d", 0.125);
        collector.verify("A", "e", 33);
        collector.verify("A", "f", 96);
        collector.verify("A", "g", 8192);
    }

    @Test
    void testConcatOperator() {
        String dsl = """
                table A
                  [a] = "a" & 1 + 2
                  [b] = "a" & 1 = "a1"
                  [c] = "a" & 1 >= 1
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "a", "a3");
        collector.verify("A", "b", "TRUE");
        collector.verify("A", "c", "TRUE");
    }

    @Test
    void testBoolLiterals() {
        String dsl = """
                table A
                  [a] = TRUE
                  [b] = FALSE
                  [c] = TRUE = TRUE
                  [d] = A(TRUE)[a]
                  [e] = A(FALSE)[a]
                """;

        ResultCollector collector = executeWithoutErrors(dsl);

        collector.verify("A", "a", "TRUE");
        collector.verify("A", "b", "FALSE");
        collector.verify("A", "c", "TRUE");
        collector.verify("A", "d", "TRUE");
        collector.verify("A", "e", Doubles.ERROR_NA);
    }

    @Test
    void testAggregationOnScalarError() {
        String dsl = """
                table A
                  [a] = 1
                  [b] = SUM([a])
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verifyError("A", "b",
                "Invalid argument \"array\" for function SUM: expected an array, but got a number. Did you mean A[a]?");
    }

    @Test
    void testDimOnScalarError() {
        String dsl = """
                table A
                  dim [a] = 1
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verifyError("A", "a",
                "Formula for column with dim keyword must return a table, an array or period series, but got a number.");
    }

    @Test
    void testDimOnRowError() {
        String dsl = """
                table A
                  dim [a] = RANGE(1)
                
                table B
                  dim [a] = A(1)
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verifyError("B", "a",
                "Formula for column with dim keyword must return a table, an array or period series, but got a row.");
    }

    @Test
    void testMissingQueryTableError() {
        String dsl = """
                table A
                  dim [a] = RANGE(5)
                
                table B
                  dim [b] = RANGE(3)
                      [f] = FILTER(A, [a] = [b])
                      [s] = SORTBY(A, [a])
                      [u] = UNIQUEBY(A, [a])
                """;

        ResultCollector collector = executeWithErrors(dsl);

        collector.verifyError("B", "f",
                "The column 'a' does not exist in the table 'B'. Did you mean A[a]?");
        collector.verifyError("B", "s",
                "The column 'a' does not exist in the table 'B'. Did you mean A[a]?");
        collector.verifyError("B", "u",
                "The column 'a' does not exist in the table 'B'. Did you mean A[a]?");
    }

    @Test
    void testAggregateAfterJoin() {
        String dsl = """
                !manual()
                table A
                  [key]
                  [val1]
                  [val2]
                override
                  [key], [val1], [val2]
                  "France", 1, 2
                  "Spain",  3, 4
                  "France", 4, 5
                  NA, 5, 6
                
                table B
                  dim [key] = {"France", "UK", NA}
                      [filter] = FILTER(A, A[key] = [key])
                      [COUNT] =  COUNT([filter][val1])
                      [SUM] = SUM([filter][val1])
                      [AVERAGE] = AVERAGE([filter][val1])
                      [MIN] = MIN([filter][val1])
                      [MAX] = MAX([filter][val1])
                      [STDEVS] = STDEVS([filter][val1])
                      [STDEVP] = STDEVP([filter][val1])
                      [GEOMEAN] = GEOMEAN([filter][val1])
                      [MEDIAN] = MEDIAN([filter][val1])
                      [MODE] = MODE([filter][val1])
                      [CORREL] = CORREL([filter][val1], [filter][val2])
                      [FIRST] = FIRST([filter][val1])
                      [LAST] = LAST([filter][val1])
                      [SINGLE] = SINGLE([filter][val1])
                      [INDEX] = INDEX([filter][val1], 1)
                      [MINBY] = MINBY([filter][val1], [filter][val2])
                      [MAXBY] = MAXBY([filter][val1], [filter][val2])
                      [FIRSTS] = FIRST([filter][val1], 1)
                      [LASTS] = LAST([filter][val1], 1)
                      [PERIODSERIES] = PERIODSERIES([filter][val1], [filter][val1], "DAY")
                """;

        ResultCollector data = executeWithoutErrors(dsl);

        data.verify("B", "filter", "2", "0", "0");
        data.verify("B", "COUNT", "2", "0", "0");
        data.verify("B", "SUM", "5.0", "0.0", "0.0");
        data.verify("B", "AVERAGE", "2.5", null, null);
        data.verify("B", "MIN", "1.0", null, null);
        data.verify("B", "MAX", "4.0", null, null);
        data.verify("B", "STDEVS", "2.1213203435596424", null, null);
        data.verify("B", "STDEVP", "1.5", null, null);
        data.verify("B", "GEOMEAN", "2.0", null, null);
        data.verify("B", "MEDIAN", "2.5", null, null);
        data.verify("B", "MODE", (String) null, null, null);
        data.verify("B", "CORREL", "1.0", null, null);

        data.verify("B", "MINBY", "1.0", null, null);
        data.verify("B", "MAXBY", "4.0", null, null);

        data.verify("B", "FIRST", "1.0", null, null);
        data.verify("B", "SINGLE", (String) null, null, null);
        data.verify("B", "LAST", "4.0", null, null);
        data.verify("B", "INDEX", "1.0", null, null);

        data.verify("B", "FIRSTS", "1", "0", "0");
        data.verify("B", "LASTS", "1", "0", "0");

        data.verify("B", "PERIODSERIES",
                new PeriodSeries(Period.DAY, 0, 1.0, Doubles.ERROR_NA, Doubles.ERROR_NA, 4.0),
                null, null);
    }

    @Test
    void testJoinSingleWithOneRow() {
        String dsl = """
                table A
                  key [a] = NA
                      [b] = 10
                      [c] = A(NA)[b]
                """;

        ResultCollector data = executeWithoutErrors(dsl);
        data.verify("A", "c", (String) null);
    }
}
