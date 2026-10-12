普通调用先在代码中写明类型和方法，例如 `student.introduce("你好")`。反射则在运行时取得类的信息，再决定调用哪个构造方法、方法或字段。注解负责给这些声明附加信息，编译器、工具或框架再读取它们。

本文示例使用 **JDK 21**。每段 Java 示例都是一个完整程序，保存为代码中 public 类同名的 `.java` 文件即可编译运行。各段独立运行，不要把不同示例中的同名辅助类合并到一起。

例如 `ReflectionDemo` 保存为 `ReflectionDemo.java`，执行：

```bash
javac -encoding UTF-8 ReflectionDemo.java
java ReflectionDemo
```

相关笔记：[Java 关键字与泛型](#/articles/java-keywords-generics)。

## 一、反射

### 1. Class 对象是什么

> **几句话看懂：** Class 描述一个类型，new 创建这个类型的对象。已知类型用 .class，有对象用 getClass，只有类名时用 Class.forName。

`Student` 是类型；`new Student()` 创建学生对象；`Student.class` 取得描述这个类型的 `Class` 对象。`Class` 对象保存的是类型信息，不是某个学生的姓名、年龄。

取得 `Class` 对象有三种常见方式：已经知道类型时用 `.class`；已经有对象时用 `getClass()`；类型名称来自配置时用 `Class.forName()`。[Class API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Class.html)

```java
class Student {}

public class ClassTokenDemo {
    public static void main(String[] args) throws ClassNotFoundException {
        Student student = new Student();
        Class<Student> classFromType = Student.class;
        Class<?> classFromObject = student.getClass();
        Class<?> classFromName = Class.forName("Student");

        System.out.println(classFromType == classFromObject);
        System.out.println(classFromObject == classFromName);
        System.out.println(classFromName.getName());
        System.out.println(int.class == Integer.class);
    }
}
```

输出：

```text
true
true
Student
false
```

这里三种方式取得的是同一个类型的信息。例子没有声明包，所以写 `"Student"`；如果是 `fanshe` 包下的 Student，名称就是 `"fanshe.Student"`。相同名称在不同类加载器中加载，可能对应不同的 `Class` 对象。

`Class<Student>` 表示已知类型是 Student，`Class<?>` 表示具体类型未知。尽量不写省略类型参数的原始类型 `Class`。`Class.forName(String)` 可能抛出 `ClassNotFoundException`，而且会初始化目标类，因此类名要来自可信配置。

### 2. Constructor、Method、Field 分别代表什么

> **几句话看懂：** Constructor 对应构造方法，Method 对应方法，Field 对应字段声明。Declared 查当前类声明的成员；不带 Declared 的这组 API 主要查 public 成员。

| 类型 | 描述的内容 | 常用操作 |
| --- | --- | --- |
| `Constructor<T>` | 一个构造方法 | `newInstance(...)` 创建对象 |
| `Method` | 一个方法 | `invoke(对象, 参数...)` 调用方法 |
| `Field` | 一个字段的声明 | `get(对象)` 读取、`set(对象, 值)` 修改 |

`Field` 本身不是字段值。例如一个 `name` 字段对应一个 Field，但张三和李四两个对象里的 name 值可以不同。[Field API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Field.html)

取得成员时要分清有没有 `Declared`：

| API | 范围 |
| --- | --- |
| `getConstructors()` / `getConstructor(...)` | 当前类的 public 构造方法；构造方法不被继承 |
| `getDeclaredConstructors()` / `getDeclaredConstructor(...)` | 当前类声明的全部构造方法，包括 private |
| `getMethods()` / `getMethod(...)` | public 方法，包括继承来的 public 方法 |
| `getDeclaredMethods()` / `getDeclaredMethod(...)` | 当前类声明的方法，包括 private；不包含继承来的方法 |
| `getFields()` / `getField(...)` | public 字段，包括继承来的 public 字段 |
| `getDeclaredFields()` / `getDeclaredField(...)` | 当前类声明的字段，包括 private；不包含继承来的字段 |

### 3. 创建对象、调用方法与修改字段

> **几句话看懂：** 先按名称和参数类型查找成员，再提供对象和实际参数来使用。构造方法用 newInstance，普通方法用 invoke，字段用 get、set。

查找构造方法时，传的是参数的**类型**，例如 `String.class, int.class`；创建对象时，传的是实际的**值**，例如 `"张三", 20`。参数类型和顺序必须与构造方法声明一致。

下面把创建对象、调用方法、修改字段串起来。Student 与 ReflectionDemo 是两个顶层类，Student 的 private 成员需要通过访问检查后才能使用。

```java
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

class Student {
    private String name;
    private int age;

    public Student() {
        this.name = "未命名";
        this.age = 0;
    }
    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }
    private Student(String name) {
        this.name = name;
        this.age = 18;
    }

    public String introduce(String prefix) {
        return prefix + name + "，" + age + "岁";
    }
    private String secret() {
        return "私有方法：" + name;
    }
    public static String typeName() {
        return "Student";
    }
}

public class ReflectionDemo {
    public static void main(String[] args) throws Exception {
        Class<Student> type = Student.class;

        Constructor<Student> publicConstructor =
            type.getConstructor(String.class, int.class);
        Student student = publicConstructor.newInstance("张三", 20);

        Method introduce = type.getMethod("introduce", String.class);
        Object result = introduce.invoke(student, "你好，");
        System.out.println(result);

        Field name = type.getDeclaredField("name");
        if (!name.trySetAccessible()) {
            throw new IllegalAccessException("不能访问 name");
        }
        name.set(student, "李四");
        System.out.println(name.get(student));
        System.out.println(introduce.invoke(student, "你好，"));

        Constructor<Student> privateConstructor =
            type.getDeclaredConstructor(String.class);
        if (!privateConstructor.trySetAccessible()) {
            throw new IllegalAccessException("不能访问私有构造方法");
        }
        Student another = privateConstructor.newInstance("王五");
        System.out.println(another.introduce("你好，"));

        Method secret = type.getDeclaredMethod("secret");
        if (!secret.trySetAccessible()) {
            throw new IllegalAccessException("不能访问 secret");
        }
        System.out.println(secret.invoke(student));
        Method typeName = type.getMethod("typeName");
        Object typeResult = typeName.invoke(null);
        System.out.println(typeResult);

        Constructor<Student> noArgumentConstructor = type.getConstructor();
        Object unknown = noArgumentConstructor.newInstance();
        System.out.println(introduce.invoke(unknown, "你好，"));
        Student known = (Student) unknown;
        System.out.println(known.introduce("你好，"));
    }
}
```

输出：

```text
你好，张三，20岁
李四
你好，李四，20岁
你好，王五，18岁
私有方法：李四
Student
你好，未命名，0岁
你好，未命名，0岁
```

`name.set(student, "李四")` 改的是 student 这个对象的 name，不会把所有学生的 name 都改掉。`unknown` 的实际对象仍然是 Student，只是变量的编译时类型写成了 Object，不能直接写 `unknown.introduce(...)`。已知真实类型时可以转型，不知道时可以用 Method 调用。

实例方法的 `invoke` 第一个参数是目标对象；静态方法不依赖实例，可以传 `null`。返回值统一用 Object 接收，基本类型会装箱，void 方法返回 `null`。[Method.invoke](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html#invoke(java.lang.Object,java.lang.Object...))

### 4. 取得 private 成员，不等于一定能使用

> **几句话看懂：** 能找到 private 成员，不代表就能调用或修改。启用访问仍受模块封装等限制，失败时要处理，不能把反射当成无限制访问。

`getDeclaredField()` 可以找到 private 字段，但读写时仍要检查访问权限。`setAccessible(true)` 请求抑制 Java 语言访问检查；`trySetAccessible()` 在不能启用时返回 false，更方便处理失败。

这不是无条件越过所有限制。JDK 的模块封装仍然有效：未开放的包可能不允许访问，`setAccessible(true)` 可能抛出 `InaccessibleObjectException`。不要默认对 JDK 内部类或所有第三方类型都能访问。[AccessibleObject API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html)

上一段完整程序分别演示了 private 字段、private 方法和 private 构造方法的访问。这里也不要把反射当成修改 final 常量的通用办法；final 字段还有额外限制，普通业务应通过构造方法和公开方法维护对象状态。

### 5. 参数类型错误与目标方法异常

> **几句话看懂：** 查找成员时，参数类型必须匹配，int.class 不等于 Integer.class。目标方法自身的异常会被包装，读取 InvocationTargetException 的 cause 才能看到原因。

查找阶段的 `int.class` 和 `Integer.class` 是不同类型，不会自动替换。调用阶段才会按规则进行装箱、拆箱或允许的基本类型扩大转换。方法本身抛出的异常，会包装在 `InvocationTargetException` 中。[Method API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Method.html)

```java
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionErrorDemo {
    public static int square(int value) {
        return value * value;
    }
    public static int divide(int a, int b) {
        return a / b;
    }

    public static void main(String[] args) throws Exception {
        Class<ReflectionErrorDemo> type = ReflectionErrorDemo.class;
        try {
            type.getMethod("square", Integer.class);
        } catch (NoSuchMethodException e) {
            System.out.println("查找失败：参数声明为 int");
        }

        Method square = type.getMethod("square", int.class);
        System.out.println(square.invoke(null, Integer.valueOf(4)));

        Method divide = type.getMethod("divide", int.class, int.class);
        try {
            divide.invoke(null, 10, 0);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            System.out.println(cause.getClass().getSimpleName());
        }
    }
}
```

输出：

```text
查找失败：参数声明为 int
16
ArithmeticException
```

常见错误可以按发生阶段区分：

| 异常 | 常见原因 |
| --- | --- |
| `ClassNotFoundException` | 类名或类加载路径不正确 |
| `NoSuchMethodException` | 方法名、构造方法参数类型或顺序不匹配 |
| `NoSuchFieldException` | 字段名错误，或字段只在父类中声明 |
| `IllegalAccessException` | 找到了成员，但当前调用不允许访问 |
| `IllegalArgumentException` | 目标对象或实际参数不适合这个成员 |
| `InvocationTargetException` | 构造方法或目标方法执行时抛出异常，检查 `getCause()` |

### 6. 根据配置选择类型和方法

> **几句话看懂：** 配置给出类名和方法名，反射负责找到类型、创建对象、执行方法。这样能动态选择行为，也需要处理运行时错误。

框架常根据配置创建对象，再调用相应的方法。下面把两条配置写在字符串里，避免运行示例时还要准备文件；实际项目也可以用 Properties 读取配置文件。

```java
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Properties;

class ConfiguredService {
    public ConfiguredService() {}

    public void printState() {
        System.out.println("服务已启动");
    }
}

public class ConfigReflectionDemo {
    public static void main(String[] args) throws Exception {
        String configText = "className=ConfiguredService\nmethodName=printState\n";
        Properties config = new Properties();
        try (StringReader reader = new StringReader(configText)) {
            config.load(reader);
        }

        String className = config.getProperty("className");
        String methodName = config.getProperty("methodName");
        Class<?> type = Class.forName(className);
        Constructor<?> constructor = type.getConstructor();
        Object service = constructor.newInstance();
        Method method = type.getMethod(methodName);
        method.invoke(service);
    }
}
```

输出：

```text
服务已启动
```

配置提供类名和方法名；反射取得类型、创建对象并调用方法。这只是说明处理过程，类名应来自可信配置，真实框架还会处理依赖和对象生命周期。

反射把部分检查推迟到运行时，还涉及查找与动态调用成本，性能取决于 JDK 和使用方式。普通业务能直接调用时优先直接调用；需要动态处理时，可缓存成员信息并处理运行时异常，不能简单说反射固定慢多少倍。

## 二、注解

### 1. 注解是什么，为什么不会自己执行

> **几句话看懂：** 注解是给程序附加的信息，不会单靠名字执行功能。编译器、工具或框架读取它，再决定做什么。

注解是附在类、方法、字段等位置上的元数据。`@Override` 由编译器检查重写，框架中的路由注解则由框架读取后注册路由。自定义 `@NotBlank` 如果没有校验器去读取，写上它也不会自动阻止空字符串。

注解用 `@interface` 定义。它是一种特殊接口，直接父接口是 `java.lang.annotation.Annotation`；不能把它理解为写一个普通 interface 再随意继承 Annotation 就得到了注解。[JLS 9.6 注解接口](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6)

### 2. 定义、使用并读取自定义注解

> **几句话看懂：** 用 @interface 定义注解，在使用处填写元素值。运行时用 getAnnotation 取得注解对象，再调用元素方法读取值。

注解元素写成没有参数的方法。使用时提供值，读取时调用同名方法。下面的 `value()` 是必填元素，`level()` 和 `tags()` 有默认值。

```java
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.Arrays;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@interface Note {
    String value();
    int level() default 1;
    String[] tags() default {};
}

@Note(value = "Java 笔记", tags = {"Java", "Java基础"})
public class AnnotationDemo {
    @Note("入门方法")
    public void study() {}

    public static void main(String[] args) throws Exception {
        Note classNote = AnnotationDemo.class.getAnnotation(Note.class);
        if (classNote != null) {
            System.out.println(classNote.value());
            System.out.println(classNote.level());
            System.out.println(Arrays.toString(classNote.tags()));
        }

        Method study = AnnotationDemo.class.getMethod("study");
        System.out.println(study.isAnnotationPresent(Note.class));
        System.out.println(study.getAnnotation(Note.class).value());
        System.out.println(AnnotationDemo.class.getMethod("main", String[].class)
            .getAnnotation(Note.class) == null);
    }
}
```

输出：

```text
Java 笔记
1
[Java, Java基础]
true
入门方法
true
```

只有给 `value` 赋值时可以省略元素名，例如 `@Note("入门方法")`。注解元素可使用基本类型、String、Class、枚举、注解类型以及这些类型的一维数组；不能写普通对象或 `List<String>`，也不能把 null 作为元素值。

`getAnnotation()` 没找到时返回 null，要先判断再调用元素方法。字段同样可以通过 `field.getAnnotation(...)` 读取。[AnnotatedElement API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AnnotatedElement.html)

### 3. 元注解：保存多久、允许写在哪

> **几句话看懂：** Retention 决定注解保存到哪个阶段，Target 决定允许标在哪。要在运行时通过普通反射读取，使用 RUNTIME。

元注解是用来标注注解定义的注解。

| 元注解 | 控制什么 |
| --- | --- |
| `@Retention` | 注解保留到哪个阶段 |
| `@Target` | 注解可以用在哪种位置 |
| `@Documented` | 使用该注解时将它包含在生成的 API 文档中，不负责执行功能 |

`@Retention` 的三种策略：

| 策略 | `.class` 文件 | 普通运行时反射 |
| --- | --- | --- |
| `SOURCE` | 不保留 | 读不到 |
| `CLASS` | 保留 | 读不到 |
| `RUNTIME` | 保留 | 可以读取 |

不写 Retention 时默认是 CLASS。CLASS 注解仍可能被字节码工具读取，并不代表所有工具都读不到。[Retention API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/annotation/Retention.html)

```java
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@interface SourceMark {}
@interface ClassMark {}
@Retention(RetentionPolicy.RUNTIME)
@interface RuntimeMark {}

@SourceMark
@ClassMark
@RuntimeMark
public class RetentionDemo {
    public static void main(String[] args) {
        Class<RetentionDemo> type = RetentionDemo.class;
        System.out.println(type.isAnnotationPresent(SourceMark.class));
        System.out.println(type.isAnnotationPresent(ClassMark.class));
        System.out.println(type.isAnnotationPresent(RuntimeMark.class));
    }
}
```

输出：

```text
false
false
true
```

这里的“作用域”要拆成两个问题：Retention 决定保存到什么时候；Target 决定允许标在哪。

| 常见 ElementType | 位置 |
| --- | --- |
| `TYPE` | 类、接口等类型声明 |
| `METHOD` | 方法 |
| `FIELD` | 字段 |
| `CONSTRUCTOR` | 构造方法 |
| `PARAMETER` | 参数声明 |
| `LOCAL_VARIABLE` | 局部变量声明 |
| `ANNOTATION_TYPE` | 注解接口声明 |
| `TYPE_USE` | 类型使用处，例如为 List 的元素类型添加标记 |
| `TYPE_PARAMETER` | 泛型类型参数声明 |
| `PACKAGE` | 包声明，通常写在 package-info.java 中 |
| `MODULE` | 模块声明 |
| `RECORD_COMPONENT` | record 的组件 |

不写 Target 不等于自动支持所有类型使用位置，应按需要明确声明。即使声明 RUNTIME，局部变量声明上的注解也不能像字段注解一样通过普通反射读取。[Target API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/annotation/Target.html)

### 4. 读取注解后，真正执行字段校验

> **几句话看懂：** 校验器先找字段上的注解，再读取该对象的字段值。检查空值和报告错误，是校验器代码执行的功能。

这个例子只检查当前类声明的 String 字段。它先找到 `@NotBlank`，再读取对象的字段值，最后判断值是否为 null 或空白。校验行为来自 `validate()`，不是来自注解名字。

```java
import java.lang.annotation.*;
import java.lang.reflect.Field;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@interface NotBlank {}

class Account {
    @NotBlank
    private String username;

    public Account(String username) {
        this.username = username;
    }
}

public class ValidationDemo {
    public static void validate(Object bean) throws IllegalAccessException {
        for (Field field : bean.getClass().getDeclaredFields()) {
            if (!field.isAnnotationPresent(NotBlank.class)) continue;
            if (field.getType() != String.class) {
                throw new IllegalArgumentException("@NotBlank 只用于 String 字段");
            }
            if (!field.trySetAccessible()) {
                throw new IllegalAccessException("不能访问 " + field.getName());
            }
            String value = (String) field.get(bean);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(field.getName() + " 不能为空");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        validate(new Account("张三"));
        System.out.println("校验通过");

        try {
            validate(new Account("   "));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}
```

输出：

```text
校验通过
username 不能为空
```

真实项目还需要约定父类字段、嵌套对象、错误信息和校验时机。这里先把“注解提供信息，读取它的代码执行功能”这条过程跑通。

### 5. 常用内置注解

> **几句话看懂：** Override 检查重写，Deprecated 标记不推荐使用，SuppressWarnings 抑制指定警告。抑制警告不会修复代码错误。

`@Override` 让编译器检查重写是否成立；`@Deprecated` 标记不推荐继续使用的 API；`@SuppressWarnings` 抑制指定的编译警告，不会修复代码里的类型或逻辑问题。

```java
public class BuiltInAnnotationDemo {
    static class Animal {
        public String speak() {
            return "声音";
        }
    }
    static class Cat extends Animal {
        @Override
        public String speak() {
            return "喵";
        }
    }

    @Deprecated(since = "1.0", forRemoval = false)
    public static String oldName() {
        return "旧方法";
    }

    @SuppressWarnings("deprecation")
    public static void main(String[] args) {
        System.out.println(new Cat().speak());
        System.out.println(oldName());
    }
}
```

输出：

```text
喵
旧方法
```

Cat 的方法若写成 `speek()`，Override 会让编译器发现它没有重写 Animal 的 speak。抑制警告应该缩小到确实理解原因的位置，不要用 `"all"` 掩盖所有警告。

### 6. 注解解析的底层过程

> **几句话看懂：** 编译器将需要保留的注解写入 class 文件，反射读取这些数据并提供注解对象。调用注解方法，取得的就是声明时填写或默认的元素值。

对于运行时读取，大致经过这些步骤：编译器把需要保留的注解写入 class 文件；运行时取得类、方法或字段的信息；反射解析注解数据；调用方通过注解接口读取元素值，再按自己的规则处理。

class 文件中的 `RuntimeVisibleAnnotations` 保存可由运行时反射读取的声明注解，`RuntimeInvisibleAnnotations` 保存另一类保留在字节码中的声明注解。参数注解、类型注解还有相应的独立属性。[JVM class 文件规范](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.7.16)

在 OpenJDK 21 的实现中，`AnnotationParser` 会利用动态代理创建注解对象，元素访问由 `AnnotationInvocationHandler` 处理。这是具体实现方式；日常代码应依赖注解接口和标准反射 API，不应依赖生成类的名字。[OpenJDK AnnotationParser 源码](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/classes/sun/reflect/annotation/AnnotationParser.java)

前面的 AnnotationDemo 演示了运行时读取：getAnnotation 取得注解对象，value、level、tags 返回元素值。编译期注解处理器也可以读取注解并生成代码，不需要等程序运行后再用反射。

## 官方资料

- [Java 21 反射包](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/package-summary.html)：类和成员信息、访问限制。
- [JLS 9.6 注解接口](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.6)：注解元素、元注解及使用规则。
- [AnnotatedElement](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AnnotatedElement.html)：直接存在、继承与重复注解的查询。
