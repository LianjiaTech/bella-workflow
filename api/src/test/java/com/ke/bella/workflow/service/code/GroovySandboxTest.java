package com.ke.bella.workflow.service.code;

import static com.ke.bella.workflow.service.Configs.MAX_EXE_MEMORY_ALLOC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * GroovySandbox 的测试类，主要测试各种类型闭包的处理
 */
public class GroovySandboxTest {

    private Map<String, Object> inputs;

    @BeforeEach
    public void setUp() {
        inputs = new HashMap<>();
    }

    /**
     * 测试简单闭包 - find 方法
     * 验证 find 方法的闭包能够正确返回结果
     */
    @Test
    public void testFindMethodClosure() {
        // 准备测试数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        Map<String, Object> data1 = new HashMap<>();
        data1.put("company_id", "123");
        data1.put("gender", "male");

        Map<String, Object> data2 = new HashMap<>();
        data2.put("company_id", "456");
        data2.put("gender", "female");

        dataList.add(data1);
        dataList.add(data2);

        inputs.put("datas", dataList);
        inputs.put("template", "123");
        inputs.put("gender", "male");

        // 测试 find 方法的闭包
        String code = "def result = datas.find{ it.company_id == template && it.gender == gender }; return result";
        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof Map, "结果应为 Map 类型");
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        Assertions.assertEquals("123", resultMap.get("company_id"), "应该找到正确的记录");
        Assertions.assertEquals("male", resultMap.get("gender"), "应该找到正确的记录");
    }

    /**
     * 测试简单闭包 - findAll 方法
     * 验证 findAll 方法的闭包能够正确过滤结果
     */
    @Test
    public void testFindAllMethodClosure() {
        // 准备测试数据
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        inputs.put("numbers", numbers);

        // 测试 findAll 方法的闭包
        String code = "def result = numbers.findAll{ it % 2 == 0 }; return result";
        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Integer> resultList = (List<Integer>) result;
        Assertions.assertEquals(5, resultList.size(), "应该有 5 个偶数");
        Assertions.assertTrue(resultList.contains(2), "结果应包含 2");
        Assertions.assertTrue(resultList.contains(4), "结果应包含 4");
        Assertions.assertTrue(resultList.contains(6), "结果应包含 6");
        Assertions.assertTrue(resultList.contains(8), "结果应包含 8");
        Assertions.assertTrue(resultList.contains(10), "结果应包含 10");
    }

    /**
     * 测试转换闭包 - collect 方法
     * 验证 collect 方法的闭包能够正确转换元素
     */
    @Test
    public void testCollectMethodClosure() {
        // 准备测试数据
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        inputs.put("numbers", numbers);

        // 测试 collect 方法的闭包
        String code = "def result = numbers.collect{ it * 2 }; return result";
        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Integer> resultList = (List<Integer>) result;
        Assertions.assertEquals(numbers.size(), resultList.size(), "结果大小应该与原列表相同");
        Assertions.assertEquals(Integer.valueOf(2), resultList.get(0), "第一个元素应该是 2");
        Assertions.assertEquals(Integer.valueOf(4), resultList.get(1), "第二个元素应该是 4");
        Assertions.assertEquals(Integer.valueOf(6), resultList.get(2), "第三个元素应该是 6");
        Assertions.assertEquals(Integer.valueOf(8), resultList.get(3), "第四个元素应该是 8");
        Assertions.assertEquals(Integer.valueOf(10), resultList.get(4), "第五个元素应该是 10");
    }

    /**
     * 测试迭代闭包 - each 方法
     * 验证 each 方法的闭包能够正确迭代元素
     */
    @Test
    public void testEachMethodClosure() {
        // 准备测试数据
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        inputs.put("numbers", numbers);

        // 测试 each 方法的闭包
        String code = "def sum = 0; numbers.each{ sum += it }; return sum";
        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof Integer, "结果应为 Integer 类型");
        Assertions.assertEquals(15, result, "总和应该是 15");
    }

    /**
     * 测试复杂闭包 - 包含多个语句和条件
     * 验证复杂闭包能够正确执行
     */
    @Test
    public void testComplexClosure() {
        // 准备测试数据
        List<Map<String, Object>> users = new ArrayList<>();

        Map<String, Object> user1 = new HashMap<>();
        user1.put("name", "张三");
        user1.put("age", 25);
        user1.put("gender", "male");

        Map<String, Object> user2 = new HashMap<>();
        user2.put("name", "李四");
        user2.put("age", 30);
        user2.put("gender", "male");

        Map<String, Object> user3 = new HashMap<>();
        user3.put("name", "王五");
        user3.put("age", 22);
        user3.put("gender", "male");

        Map<String, Object> user4 = new HashMap<>();
        user4.put("name", "赵六");
        user4.put("age", 35);
        user4.put("gender", "male");

        users.add(user1);
        users.add(user2);
        users.add(user3);
        users.add(user4);

        inputs.put("users", users);

        // 测试复杂闭包
        String code = "def result = users.findAll { user ->\n" +
                "    def isAdult = user.age >= 18\n" +
                "    def isYoung = user.age < 30\n" +
                "    \n" +
                "    if (isAdult && isYoung) {\n" +
                "        return true\n" +
                "    } else {\n" +
                "        return false\n" +
                "    }\n" +
                "};\n" +
                "return result.collect { it.name }";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<String> resultList = (List<String>) result;
        Assertions.assertEquals(2, resultList.size(), "应该有 2 个用户");
        Assertions.assertTrue(resultList.contains("张三"), "结果应包含张三");
        Assertions.assertTrue(resultList.contains("王五"), "结果应包含王五");
    }

    /**
     * 测试嵌套闭包
     * 验证嵌套闭包能够正确执行
     */
    @Test
    public void testNestedClosure() {
        // 准备测试数据
        List<List<Integer>> nestedNumbers = new ArrayList<>();
        nestedNumbers.add(Arrays.asList(1, 2, 3));
        nestedNumbers.add(Arrays.asList(4, 5, 6));
        nestedNumbers.add(Arrays.asList(7, 8, 9));

        inputs.put("nestedNumbers", nestedNumbers);

        // 测试嵌套闭包
        String code = "def result = nestedNumbers.collect { list ->\n" +
                "    list.findAll { num ->\n" +
                "        num % 2 == 0\n" +
                "    }\n" +
                "};\n" +
                "return result";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<List<Integer>> resultList = (List<List<Integer>>) result;
        Assertions.assertEquals(3, resultList.size(), "应该有 3 个子列表");

        // 验证第一个子列表
        List<Integer> subList1 = resultList.get(0);
        Assertions.assertEquals(1, subList1.size(), "第一个子列表应该有 1 个元素");
        Assertions.assertTrue(subList1.contains(2), "第一个子列表应包含 2");

        // 验证第二个子列表
        List<Integer> subList2 = resultList.get(1);
        Assertions.assertEquals(2, subList2.size(), "第二个子列表应该有 2 个元素");
        Assertions.assertTrue(subList2.contains(4), "第二个子列表应包含 4");
        Assertions.assertTrue(subList2.contains(6), "第二个子列表应包含 6");

        // 验证第三个子列表
        List<Integer> subList3 = resultList.get(2);
        Assertions.assertEquals(1, subList3.size(), "第三个子列表应该有 1 个元素");
        Assertions.assertTrue(subList3.contains(8), "第三个子列表应包含 8");
    }

    /**
     * 测试带有方法调用的闭包
     * 验证带有方法调用的闭包能够正确执行
     */
    @Test
    public void testClosureWithMethodCalls() {
        // 准备测试数据
        List<String> words = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        inputs.put("words", words);

        // 测试带有方法调用的闭包
        String code = "def result = words.findAll { word ->\n" +
                "    word.length() > 5 && word.contains('a')\n" +
                "};\n" +
                "return result";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<String> resultList = (List<String>) result;
        Assertions.assertEquals(1, resultList.size(), "应该有 2 个单词");
        Assertions.assertTrue(resultList.contains("banana"), "结果应包含 banana");
    }

    /**
     * 测试迭代闭包 - forEach 方法
     * 验证 forEach 方法的闭包能够正确迭代元素
     */
    @Test
    public void testForEachMethodClosure() {
        // 准备测试数据
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        inputs.put("numbers", numbers);

        // 测试 forEach 方法的闭包
        String code = "def sum = 0; numbers.forEach{ num -> sum += num }; return sum";
        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof Integer, "结果应为 Integer 类型");
        Assertions.assertEquals(Integer.valueOf(15), result, "总和应该是 15");
    }

    /**
     * 测试转换闭包 - map 方法
     * 验证 map 方法的闭包能够正确转换元素
     */
    @Test
    public void testMapMethodClosure() {
        // 准备测试数据
        List<String> words = Arrays.asList("a", "bb", "ccc", "dddd");
        inputs.put("words", words);

        // 测试 map 方法的闭包
        String code = "def result = words.stream().map{ it.length() }.collect(java.util.stream.Collectors.toList()); return result";
        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Integer> resultList = (List<Integer>) result;
        Assertions.assertEquals(words.size(), resultList.size(), "结果大小应该与原列表相同");
        Assertions.assertEquals(Integer.valueOf(1), resultList.get(0), "第一个元素应该是 1");
        Assertions.assertEquals(Integer.valueOf(2), resultList.get(1), "第二个元素应该是 2");
        Assertions.assertEquals(Integer.valueOf(3), resultList.get(2), "第三个元素应该是 3");
        Assertions.assertEquals(Integer.valueOf(4), resultList.get(3), "第四个元素应该是 4");
    }

    /**
     * 测试带有异常处理的闭包
     * 验证带有异常处理的闭包能够正确执行
     */
    @Test
    public void testClosureWithExceptionHandling() {
        // 准备测试数据
        List<String> inputs = Arrays.asList("1", "2", "a", "3", "b", "4");
        this.inputs.put("inputs", inputs);

        // 测试带有异常处理的闭包
        String code = "def result = []\n" +
                "inputs.each { item ->\n" +
                "    try {\n" +
                "        def num = Integer.parseInt(item)\n" +
                "        result << num * 2\n" +
                "    } catch (NumberFormatException e) {\n" +
                "        result << 'error'\n" +
                "    }\n" +
                "}\n" +
                "return result";

        Object result = GroovySandbox.execute(code, this.inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) result;
        Assertions.assertEquals(inputs.size(), resultList.size(), "结果大小应该与原列表相同");
        Assertions.assertEquals(2, resultList.get(0), "第一个元素应该是 2");
        Assertions.assertEquals(4, resultList.get(1), "第二个元素应该是 4");
        Assertions.assertEquals("error", resultList.get(2), "第三个元素应该是 'error'");
        Assertions.assertEquals(6, resultList.get(3), "第四个元素应该是 6");
        Assertions.assertEquals("error", resultList.get(4), "第五个元素应该是 'error'");
        Assertions.assertEquals(8, resultList.get(5), "第六个元素应该是 8");
    }

    /**
     * 测试多重嵌套闭包
     * 验证多重嵌套闭包能够正确执行
     */
    @Test
    public void testMultiLevelNestedClosure() {
        // 准备测试数据
        Map<String, List<Integer>> data = new HashMap<>();
        data.put("odd", Arrays.asList(1, 3, 5, 7, 9));
        data.put("even", Arrays.asList(2, 4, 6, 8, 10));
        inputs.put("data", data);

        // 测试多重嵌套闭包
        String code = "def result = data.collect { category, numbers ->\n" +
                "    def processedNumbers = numbers.collect { num ->\n" +
                "        def squared = num * num\n" +
                "        def isLarge = squared > 50\n" +
                "        return [\n" +
                "            original: num,\n" +
                "            squared: squared,\n" +
                "            isLarge: isLarge\n" +
                "        ]\n" +
                "    }\n" +
                "    return [category: category, numbers: processedNumbers]\n" +
                "}\n" +
                "return result";

        Object result = GroovySandbox.execute(code, inputs, 5000, MAX_EXE_MEMORY_ALLOC);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) result;
        Assertions.assertEquals(2, resultList.size(), "应该有 2 个分类");

        // 验证每个分类的结果
        for (Map<String, Object> categoryResult : resultList) {
            String category = (String) categoryResult.get("category");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> numbers = (List<Map<String, Object>>) categoryResult.get("numbers");

            Assertions.assertTrue(category.equals("odd") || category.equals("even"), "分类应该是 'odd' 或 'even'");
            Assertions.assertEquals(5, numbers.size(), "每个分类应该有 5 个数字");

            // 验证每个数字的处理结果
            for (Map<String, Object> numResult : numbers) {
                Integer original = (Integer) numResult.get("original");
                Integer squared = (Integer) numResult.get("squared");
                Boolean isLarge = (Boolean) numResult.get("isLarge");

                Assertions.assertEquals(original * original, squared.intValue(), "平方值应该正确");
                Assertions.assertEquals(squared > 50, isLarge, "isLarge 标志应该正确");
            }
        }
    }

    /**
     * 测试带有闭包作为参数的闭包
     * 验证带有闭包作为参数的闭包能够正确执行
     */
    @Test
    public void testClosureWithClosureParameter() {
        // 准备测试数据
        inputs.put("multiplier", 2);

        // 测试带有闭包作为参数的闭包
        String code = "def applyOperation = { int x, Closure operation ->\n" +
                "    return operation(x)\n" +
                "}\n" +
                "\n" +
                "def multiply = { int x -> x * multiplier }\n" +
                "def square = { int x -> x * x }\n" +
                "\n" +
                "def result1 = applyOperation(5, multiply)\n" +
                "def result2 = applyOperation(5, square)\n" +
                "\n" +
                "return [result1, result2]";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Integer> resultList = (List<Integer>) result;
        Assertions.assertEquals(2, resultList.size(), "应该有 2 个结果");
        Assertions.assertEquals(Integer.valueOf(10), resultList.get(0), "第一个结果应该是 10 (5 * 2)");
        Assertions.assertEquals(Integer.valueOf(25), resultList.get(1), "第二个结果应该是 25 (5 * 5)");
    }

    /**
     * 测试带有显式返回语句的闭包
     * 验证带有显式返回语句的闭包能够正确执行
     */
    @Test
    public void testClosureWithExplicitReturn() {
        // 准备测试数据
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        inputs.put("numbers", numbers);

        // 测试带有显式返回语句的闭包
        String code = "def result = numbers.collect { num ->\n" +
                "    if (num % 2 == 0) {\n" +
                "        return 'even'\n" +
                "    } else {\n" +
                "        return 'odd'\n" +
                "    }\n" +
                "}\n" +
                "return result";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<String> resultList = (List<String>) result;
        Assertions.assertEquals(numbers.size(), resultList.size(), "结果大小应该与原列表相同");
        Assertions.assertEquals("odd", resultList.get(0), "第一个元素应该是 'odd'");
        Assertions.assertEquals("even", resultList.get(1), "第二个元素应该是 'even'");
        Assertions.assertEquals("odd", resultList.get(2), "第三个元素应该是 'odd'");
        Assertions.assertEquals("even", resultList.get(3), "第四个元素应该是 'even'");
    }

    /**
     * 测试带有循环的闭包
     * 验证带有循环的闭包能够正确执行
     */
    @Test
    public void testClosureWithLoop() {
        // 准备测试数据
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        inputs.put("numbers", numbers);

        // 测试带有循环的闭包
        String code = "def result = numbers.collect { num ->\n" +
                "    def factorial = 1\n" +
                "    for (int i = 1; i <= num; i++) {\n" +
                "        factorial *= i\n" +
                "    }\n" +
                "    return factorial\n" +
                "}\n" +
                "return result";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Integer> resultList = (List<Integer>) result;
        Assertions.assertEquals(numbers.size(), resultList.size(), "结果大小应该与原列表相同");
        Assertions.assertEquals(Integer.valueOf(1), resultList.get(0), "第一个元素应该是 1! = 1");
        Assertions.assertEquals(Integer.valueOf(2), resultList.get(1), "第二个元素应该是 2! = 2");
        Assertions.assertEquals(Integer.valueOf(6), resultList.get(2), "第三个元素应该是 3! = 6");
        Assertions.assertEquals(Integer.valueOf(24), resultList.get(3), "第四个元素应该是 4! = 24");
        Assertions.assertEquals(Integer.valueOf(120), resultList.get(4), "第五个元素应该是 5! = 120");
    }

    /**
     * 测试带有 switch 语句的闭包
     * 验证带有 switch 语句的闭包能够正确执行
     */
    @Test
    public void testClosureWithSwitchStatement() {
        // 准备测试数据
        List<String> fruits = Arrays.asList("apple", "banana", "orange", "grape", "watermelon");
        inputs.put("fruits", fruits);

        // 测试带有 switch 语句的闭包
        String code = "def result = fruits.collect { fruit ->\n" +
                "    def category\n" +
                "    switch (fruit) {\n" +
                "        case 'apple':\n" +
                "        case 'grape':\n" +
                "            category = 'small'\n" +
                "            break\n" +
                "        case 'banana':\n" +
                "        case 'orange':\n" +
                "            category = 'medium'\n" +
                "            break\n" +
                "        case 'watermelon':\n" +
                "            category = 'large'\n" +
                "            break\n" +
                "        default:\n" +
                "            category = 'unknown'\n" +
                "    }\n" +
                "    return [name: fruit, category: category]\n" +
                "}\n" +
                "return result";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> resultList = (List<Map<String, String>>) result;
        Assertions.assertEquals(fruits.size(), resultList.size(), "结果大小应该与原列表相同");

        // 验证每个水果的分类
        for (Map<String, String> fruitResult : resultList) {
            String name = fruitResult.get("name");
            String category = fruitResult.get("category");

            switch (name) {
            case "apple":
            case "grape":
                Assertions.assertEquals("small", category, "应该分类为 'small'");
                break;
            case "banana":
            case "orange":
                Assertions.assertEquals("medium", category, "应该分类为 'medium'");
                break;
            case "watermelon":
                Assertions.assertEquals("large", category, "应该分类为 'large'");
                break;
            default:
                Assertions.fail("不应该有未知的水果");
            }
        }
    }

    /**
     * 测试带有递归的闭包
     * 验证带有递归的闭包能够正确执行
     */
    @Test
    public void testClosureWithRecursion() {
        // 准备测试数据
        inputs.put("n", 5);

        // 测试带有递归的闭包
        String code = "def factorial\n" +
                "factorial = { int n ->\n" +
                "    if (n <= 1) {\n" +
                "        return 1\n" +
                "    } else {\n" +
                "        return n * factorial(n - 1)\n" +
                "    }\n" +
                "}\n" +
                "return factorial(n)";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof Integer, "结果应为 Integer 类型");
        Assertions.assertEquals(Integer.valueOf(120), result, "5! 应该等于 120");
    }

    /**
     * 测试带有多个参数的闭包
     * 验证带有多个参数的闭包能够正确执行
     */
    @Test
    public void testClosureWithMultipleParameters() {
        // 准备测试数据
        inputs.put("a", 10);
        inputs.put("b", 20);
        inputs.put("c", 30);

        // 测试带有多个参数的闭包
        String code = "def calculate = { x, y, z -> x + y * z }\n" +
                "return calculate(a, b, c)";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof Integer, "结果应为 Integer 类型");
        Assertions.assertEquals(Integer.valueOf(610), result, "10 + 20 * 30 应该等于 610");
    }

    /**
     * 测试带有闭包委托的闭包
     * 验证带有闭包委托的闭包能够正确执行
     */
    @Test
    public void testClosureWithDelegate() {
        // 准备测试数据
        Map<String, Object> person = new HashMap<>();
        person.put("name", "John");
        person.put("age", 30);
        inputs.put("person", person);

        // 测试带有闭包委托的闭包
        String code = "def printInfo = { ->\n" +
                "    return \"Name: ${name}, Age: ${age}\"\n" +
                "}\n" +
                "printInfo.delegate = person\n" +
                "printInfo.resolveStrategy = Closure.DELEGATE_FIRST\n" +
                "return printInfo()";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertEquals("Name: John, Age: 30", result.toString(), "结果应该正确");
    }

    /**
     * 测试带有闭包属性访问的闭包
     * 验证带有闭包属性访问的闭包能够正确执行
     */
    @Test
    public void testClosureWithPropertyAccess() {
        // 准备测试数据
        Map<String, Object> user = new HashMap<>();
        user.put("name", "Smith");
        Map<String, Object> address = new HashMap<>();
        address.put("city", "New York");
        address.put("district", "Manhattan");
        user.put("address", address);
        inputs.put("user", user);

        // 测试带有闭包属性访问的闭包
        String code = "def result = user.with { u ->\n" +
                "    def cityInfo = u.address.with { addr ->\n" +
                "        return \"${addr.city} ${addr.district}\"\n" +
                "    }\n" +
                "    return [name: u.name, location: cityInfo]\n" +
                "}\n" +
                "return result";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof Map, "结果应为 Map 类型");
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        Assertions.assertEquals("Smith", resultMap.get("name"), "姓名应该正确");
        Assertions.assertEquals("New York Manhattan", resultMap.get("location").toString(), "地址应该正确");
    }

    /**
     * 测试带有闭包方法调用的闭包
     * 验证带有闭包方法调用的闭包能够正确执行
     */
    @Test
    public void testClosureWithMethodInvocation() {
        // 准备测试数据
        inputs.put("text", "Hello, World!");

        // 测试带有闭包方法调用的闭包
        String code = "def processText = { String text ->\n" +
                "    def words = text.split(/\\W+/).findAll { it.length() > 0 }\n" +
                "    def counts = words.collect { word ->\n" +
                "        [word: word.toLowerCase(), length: word.length()]\n" +
                "    }\n" +
                "    return counts\n" +
                "}\n" +
                "return processText(text)";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> resultList = (List<Map<String, Object>>) result;
        Assertions.assertEquals(2, resultList.size(), "应该有 2 个单词");

        Map<String, Object> firstWord = resultList.get(0);
        Assertions.assertEquals("hello", firstWord.get("word"), "第一个单词应该是 'hello'");
        Assertions.assertEquals(5, firstWord.get("length"), "第一个单词长度应该是 5");

        Map<String, Object> secondWord = resultList.get(1);
        Assertions.assertEquals("world", secondWord.get("word"), "第二个单词应该是 'world'");
        Assertions.assertEquals(5, secondWord.get("length"), "第二个单词长度应该是 5");
    }

    /**
     * 测试带有闭包变量作用域的闭包
     * 验证带有闭包变量作用域的闭包能够正确执行
     */
    @Test
    public void testClosureWithVariableScope() {
        // 测试带有闭包变量作用域的闭包
        String code = "def outerVar = 10\n" +
                "def createClosure = { ->\n" +
                "    def innerVar = 20\n" +
                "    return { ->\n" +
                "        return [outer: outerVar, inner: innerVar]\n" +
                "    }\n" +
                "}\n" +
                "def closure = createClosure()\n" +
                "return closure()";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof Map, "结果应为 Map 类型");
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        Assertions.assertEquals(10, resultMap.get("outer"), "外部变量值应该是 10");
        Assertions.assertEquals(20, resultMap.get("inner"), "内部变量值应该是 20");
    }

    /**
     * 测试带有闭包多重返回的闭包
     * 验证带有闭包多重返回的闭包能够正确执行
     */
    @Test
    public void testClosureWithMultipleReturns() {
        // 准备测试数据
        List<Integer> numbers = Arrays.asList(-2, -1, 0, 1, 2);
        inputs.put("numbers", numbers);

        // 测试带有闭包多重返回的闭包
        String code = "def result = numbers.collect { num ->\n" +
                "    if (num < 0) return 'negative'\n" +
                "    else if (num == 0) return 'zero'\n" +
                "    else return 'positive'\n" +
                "}\n" +
                "return result";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<String> resultList = (List<String>) result;
        Assertions.assertEquals(numbers.size(), resultList.size(), "结果大小应该与原列表相同");
        Assertions.assertEquals("negative", resultList.get(0), "第一个元素应该是 'negative'");
        Assertions.assertEquals("negative", resultList.get(1), "第二个元素应该是 'negative'");
        Assertions.assertEquals("zero", resultList.get(2), "第三个元素应该是 'zero'");
        Assertions.assertEquals("positive", resultList.get(3), "第四个元素应该是 'positive'");
        Assertions.assertEquals("positive", resultList.get(4), "第五个元素应该是 'positive'");
    }

    /**
     * 测试带有闭包链式调用的闭包
     * 验证带有闭包链式调用的闭包能够正确执行
     */
    @Test
    public void testClosureWithChaining() {
        // 准备测试数据
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        inputs.put("numbers", numbers);

        // 测试带有闭包链式调用的闭包
        String code = "def result = numbers\n" +
                "    .findAll { it % 2 == 0 }\n" +
                "    .collect { it * it }\n" +
                "    .findAll { it > 20 }\n" +
                "    .collect { Math.sqrt(it) }\n" +
                "return result";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Double> resultList = (List<Double>) result;
        Assertions.assertEquals(3, resultList.size(), "应该有 3 个元素");

        // 验证结果：偶数 -> 平方 -> 大于20 -> 开方
        // 6^2 = 36 -> sqrt(36) = 6
        // 8^2 = 64 -> sqrt(64) = 8
        // 10^2 = 100 -> sqrt(100) = 10
        Assertions.assertEquals(6.0, resultList.get(0), 0.001, "第一个元素应该是 6.0");
        Assertions.assertEquals(8.0, resultList.get(1), 0.001, "第二个元素应该是 8.0");
        Assertions.assertEquals(10.0, resultList.get(2), 0.001, "第三个元素应该是 10.0");
    }

    /**
     * 测试带有闭包并行处理的闭包
     * 验证带有闭包并行处理的闭包能够正确执行
     */
    @Test
    public void testClosureWithParallelProcessing() {
        // 准备测试数据
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            numbers.add(i);
        }
        inputs.put("numbers", numbers);

        // 测试带有闭包并行处理的闭包
        String code = "def result = numbers\n" +
                "    .stream()\n" +
                "    .filter { it % 2 == 0 }\n" +
                "    .map { it * 2 }\n" +
                "    .collect(java.util.stream.Collectors.toList())\n" +
                "return result";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Integer> resultList = (List<Integer>) result;
        Assertions.assertEquals(50, resultList.size(), "应该有 50 个元素");

        // 验证结果：偶数 * 2
        for (int i = 0; i < resultList.size(); i++) {
            int expected = (i + 1) * 2 * 2; // (i+1)是序号，*2是因为是偶数，再*2是因为map操作
            Assertions.assertEquals(expected, resultList.get(i).intValue(), "第 " + (i + 1) + " 个元素应该是 " + expected);
        }
    }

    /**
     * 测试带有闭包懒加载的闭包
     * 验证带有闭包懒加载的闭包能够正确执行
     */
    @Test
    public void testClosureWithLazyEvaluation() {
        // 测试带有闭包懒加载的闭包
        String code = "def lazyValue = { ->\n" +
                "    println 'Computing value...'\n" +
                "    return 42\n" +
                "}\n" +
                "\n" +
                "def memo = [value: null, computed: false]\n" +
                "\n" +
                "def getValue = { ->\n" +
                "    if (!memo.computed) {\n" +
                "        memo.value = lazyValue()\n" +
                "        memo.computed = true\n" +
                "    }\n" +
                "    return memo.value\n" +
                "}\n" +
                "\n" +
                "// 第一次调用会计算值\n" +
                "def result1 = getValue()\n" +
                "// 第二次调用会使用缓存的值\n" +
                "def result2 = getValue()\n" +
                "\n" +
                "return [result1, result2, memo.computed]";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Object> resultList = (List<Object>) result;
        Assertions.assertEquals(3, resultList.size(), "应该有 3 个元素");
        Assertions.assertEquals(42, resultList.get(0), "第一个结果应该是 42");
        Assertions.assertEquals(42, resultList.get(1), "第二个结果应该是 42");
        Assertions.assertEquals(true, resultList.get(2), "memo.computed 应该是 true");
    }

    /**
     * 测试带有闭包柯里化的闭包
     * 验证带有闭包柯里化的闭包能够正确执行
     */
    @Test
    public void testClosureWithCurrying() {
        // 测试带有闭包柯里化的闭包
        String code = "def multiply = { a, b, c -> a * b * c }\n" +
                "\n" +
                "// 柯里化：固定第一个参数为2\n" +
                "def multiplyBy2 = multiply.curry(2)\n" +
                "\n" +
                "// 柯里化：固定第一个参数为2，第二个参数为3\n" +
                "def multiplyBy2And3 = multiply.curry(2, 3)\n" +
                "\n" +
                "def result1 = multiplyBy2(3, 4)      // 2 * 3 * 4\n" +
                "def result2 = multiplyBy2And3(4)     // 2 * 3 * 4\n" +
                "\n" +
                "return [result1, result2]";

        Object result = GroovySandbox.execute(code, inputs, 5000, 1024 * 1024 * 10);

        // 验证结果
        Assertions.assertNotNull(result, "结果不应为空");
        Assertions.assertTrue(result instanceof List, "结果应为 List 类型");
        @SuppressWarnings("unchecked")
        List<Integer> resultList = (List<Integer>) result;
        Assertions.assertEquals(2, resultList.size(), "应该有 2 个元素");
        Assertions.assertEquals(24, resultList.get(0).intValue(), "第一个结果应该是 24 (2*3*4)");
        Assertions.assertEquals(24, resultList.get(1).intValue(), "第二个结果应该是 24 (2*3*4)");
    }
}
