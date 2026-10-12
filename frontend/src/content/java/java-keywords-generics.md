访问控制决定成员能在哪里使用，final 限制再次赋值或继承，static 把成员与类关联，泛型则把类型约束写进代码。这些概念经常一起出现，但解决的是不同的问题。

本文按 **JDK 21** 说明。单文件示例保存为 public 类同名的 `.java` 文件，使用 `javac -encoding UTF-8 文件名.java` 编译，再用 `java 类名` 运行。注释中标明“不能编译”的行用于说明限制，运行时保持注释即可。

相关笔记：[Java 反射与注解](#/articles/java-reflection-annotations)。

## 一、访问控制关键字

public、protected、private 是访问修饰符。不写访问修饰符时，成员具有包访问权限，常被称作“默认访问”或 package-private。这里的“默认”不是在字段或类前写 `default` 关键字；接口里的 default 方法是另一件事。

| 成员的访问权限 | 同类 | 同包其他类 | 不同包的子类 | 不同包的非子类 |
| --- | --- | --- | --- | --- |
| public | 可以 | 可以 | 可以 | 可以 |
| protected | 可以 | 可以 | 可以，但有访问方式限制 | 不可以 |
| 不写修饰符 | 可以 | 可以 | 不可以 | 不可以 |
| private | 可以 | 不可以 | 不可以 | 不可以 |

这个表针对普通类的成员。顶层类只能使用 public 或包访问权限；private 成员也可以由同一顶层类内的嵌套类型访问。还要保证成员所属的类本身可访问。[JLS 6.6 访问控制](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6)

### 1. 同类与同包访问

> **几句话看懂：** 同一个类可以使用自己的所有成员。同包其他类能直接使用 public、protected 和包访问成员，但不能直接访问另一个顶层类的 private 成员。

下面四个文件属于一个示例。先定义 `demo.base.Parent`：

```java
// 文件：access/demo/base/Parent.java
package demo.base;

public class Parent {
    public String publicValue = "public";
    protected String protectedValue = "protected";
    String packageValue = "package-private";
    private String privateValue = "private";

    public void showInside() {
        System.out.println(publicValue + " / " + protectedValue
            + " / " + packageValue + " / " + privateValue);
    }
}
```

同包的 `SamePackage` 可以访问 public、protected 和包访问成员，但不能直接访问 Parent 的 private 字段：

```java
// 文件：access/demo/base/SamePackage.java
package demo.base;

public class SamePackage {
    public static void main(String[] args) {
        Parent parent = new Parent();
        parent.showInside();
        System.out.println(parent.publicValue);
        System.out.println(parent.protectedValue);
        System.out.println(parent.packageValue);
        // System.out.println(parent.privateValue); // 不能编译
    }
}
```

SamePackage 的输出：

```text
public / protected / package-private / private
public
protected
package-private
```

### 2. 不同包的 protected 访问

> **几句话看懂：** 跨包的普通类只能直接使用可访问类型的 public 成员。子类还能使用继承的 protected 实例成员，但访问对象的编译时类型也必须满足要求。

不同包的子类能通过自己的实例访问继承来的 protected 成员，但不能因此访问任意 Parent 对象的 protected 字段。

```java
// 文件：access/demo/other/Child.java
package demo.other;

import demo.base.Parent;

public class Child extends Parent {
    public void show(Child another, Parent parent) {
        System.out.println(this.protectedValue);
        System.out.println(another.protectedValue);
        System.out.println(parent.publicValue);
        // System.out.println(parent.protectedValue); // 不能编译
        // System.out.println(this.packageValue);     // 不同包，不能编译
        // System.out.println(this.privateValue);     // 不能编译
    }

    public static void main(String[] args) {
        new Child().show(new Child(), new Parent());
    }
}
```

Child 的输出：

```text
protected
protected
public
```

关键是**访问发生在哪里，以及限定对象的编译时类型是什么**。在 Child 中，`this` 和 `another` 都是 Child；`parent` 的声明类型是 Parent，不满足这里跨包访问 protected 实例成员的要求。[JLS 6.6.2 protected](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6.2)

不同包的普通类只能直接访问 public 成员：

```java
// 文件：access/demo/other/Outsider.java
package demo.other;

import demo.base.Parent;

public class Outsider {
    public static void main(String[] args) {
        Parent parent = new Parent();
        System.out.println(parent.publicValue);
        // System.out.println(parent.protectedValue); // 不能编译
        // System.out.println(parent.packageValue);   // 不能编译
        // System.out.println(parent.privateValue);   // 不能编译
    }
}
```

Outsider 的输出是 `public`。把四个文件按注释中的目录保存，在 access 目录执行：

```bash
javac -encoding UTF-8 -d out demo/base/Parent.java demo/base/SamePackage.java demo/other/Child.java demo/other/Outsider.java
java -cp out demo.base.SamePackage
java -cp out demo.other.Child
java -cp out demo.other.Outsider
```

## 二、final

final 修饰不同对象时，限制不同：

| 修饰位置 | 含义 |
| --- | --- |
| 类 | 不能再被继承 |
| 方法 | 不能被子类重写；仍可被继承，也可存在同名的重载方法 |
| 变量 | 赋值后不能再次赋值 |

### 1. final 引用与对象内容

> **几句话看懂：** final 变量赋值后不能再赋值。变量指向的对象如果允许修改，内容仍然可以改变。

如果变量保存的是对象引用，final 限制的是这个变量不能重新指向另一个对象。对象内部的字段仍可以通过它允许的方法修改。

```java
import java.util.ArrayList;
import java.util.List;

public class FinalReferenceDemo {
    static class Person {
        private int age;
        Person(int age) {
            this.age = age;
        }
        void setAge(int age) {
            this.age = age;
        }
        int getAge() {
            return age;
        }
    }

    public static void main(String[] args) {
        final int count = 3;
        // count = 4; // 不能再次赋值

        final Person person = new Person(18);
        person.setAge(20);
        // person = new Person(30); // 不能重新指向另一个对象

        final List<String> names = new ArrayList<>();
        names.add("张三");
        // names = new ArrayList<>(); // 不能重新赋值

        System.out.println(count);
        System.out.println(person.getAge());
        System.out.println(names);
    }
}
```

输出：

```text
3
20
[张三]
```

所以 final 变量不等于不可变对象。上面的 Person 和 ArrayList 都能改变内容。

### 2. final 字段不一定要在声明时赋值

> **几句话看懂：** final 字段可以在构造方法中完成一次赋值。final 类不能继承，final 方法不能重写，但方法仍可被继承或重载。

没有在声明处初始化的 final 实例字段，可以在构造方法中赋值。编译器会检查它是否在构造完成前被确定赋值，以及是否可能被重复赋值。

```java
public class FinalMemberDemo {
    public static final class Token {
        private final String value;
        public Token(String value) {
            this.value = value;
        }
        public String value() {
            return value;
        }
    }
    // static class SpecialToken extends Token {} // final 类不能继承

    static class Parent {
        public final String show(String value) {
            return "继承的方法：" + value;
        }
    }
    static class Child extends Parent {
        // public String show(String value) { return value; } // 不能重写
        public String show(int value) {
            return "重载的方法：" + value;
        }
    }

    public static void main(String[] args) {
        System.out.println(new Token("abc").value());
        Child child = new Child();
        System.out.println(child.show("Java"));
        System.out.println(child.show(21));
    }
}
```

输出：

```text
abc
继承的方法：Java
重载的方法：21
```

重写要求子类提供相同签名的方法实现；重载则是同名方法的参数列表不同。final 限制前者，不禁止后者。[JLS 8 类与成员](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.1.2)

## 三、static

static 主要修饰字段、方法、代码块和嵌套类。静态成员与类关联，实例成员与对象关联。

### 1. 静态字段与实例字段

> **几句话看懂：** 静态字段由同一个已加载类共享，实例字段每个对象各有一份。静态方法没有 this，访问实例成员时必须指明对象。

同一个已加载的类中，静态字段由该类的实例共享；实例字段则每个对象各有一份。通常用类名访问静态字段，这样更容易看出它属于类。

```java
public class StaticDemo {
    static class User {
        static int count = 0;
        String name;

        User(String name) {
            this.name = name;
            count++;
        }

        static int total() {
            return count;
        }
        static void show(User user) {
            // System.out.println(name); // 没有当前实例，不能直接访问
            System.out.println(user.name); // 指明对象后可以访问
        }
        void introduce() {
            System.out.println(name + " / 总人数：" + count);
        }
    }

    public static void main(String[] args) {
        User a = new User("张三");
        User b = new User("李四");
        System.out.println(User.total());
        User.show(a);
        a.introduce();
        b.introduce();
    }
}
```

输出：

```text
2
张三
张三 / 总人数：2
李四 / 总人数：2
```

不要把共享字段理解为“每个对象内部都有一个指针字段”。这里要记住的是：修改 User.count，会影响这个类共享的计数；修改 a.name，不会修改 b.name。

静态方法没有当前实例，也没有 `this`。它不能直接使用实例成员，但可以先取得一个对象，再通过该对象访问实例字段或调用实例方法。因此“静态方法完全不能调用非静态方法”并不准确。

### 2. 静态代码块

> **几句话看懂：** 静态代码块在类初始化时执行，不会随每次创建对象重复执行。它与静态字段初始化器按代码顺序执行。

静态代码块在类初始化时执行。静态字段初始化器与静态代码块按代码中的顺序执行；在这个例子里，只初始化一次，创建第二个对象不会重新执行静态块。

```java
public class StaticBlockDemo {
    static int value = 10;
    static {
        value += 5;
        System.out.println("静态块：" + value);
    }

    public StaticBlockDemo() {
        System.out.println("构造方法");
    }

    public static void main(String[] args) {
        new StaticBlockDemo();
        new StaticBlockDemo();
        System.out.println(value);
    }
}
```

输出：

```text
静态块：15
构造方法
构造方法
15
```

这里描述的是类初始化，不是简单地说“程序启动时所有静态块都执行”。例如静态方法首次调用等主动使用会触发类初始化。[JLS 12.4 类初始化](https://docs.oracle.com/javase/specs/jls/se21/html/jls-12.html#jls-12.4)

## 四、成员内部类与静态嵌套类

> **几句话看懂：** 成员内部类创建时需要外部对象，能直接使用这个对象的成员。静态嵌套类不需要外部对象，访问外部实例成员时要明确提供对象。

写在另一个类内部的类，常用来组织关系紧密的类型。是否有 static，决定它是否需要外部类实例。

| 类型 | 创建时需要外部对象 | 访问外部类实例成员 |
| --- | --- | --- |
| 成员内部类（非 static） | 需要 | 可以直接访问所属外部对象的成员 |
| 静态嵌套类 | 不需要 | 必须明确取得外部对象，不能直接使用外部实例成员 |

两者都可以访问外部类的 private 成员，但静态嵌套类访问 private 实例成员时也必须指定对象。它没有自动关联的外部实例。

```java
public class NestedClassDemo {
    private String name;
    private static String school = "Java课堂";

    public NestedClassDemo(String name) {
        this.name = name;
    }

    class Inner {
        String describe() {
            return name + " / " + school;
        }

        // JDK 16 起，内部类允许声明静态成员；这里按 JDK 21 运行。
        static String label() {
            return "Inner 的静态方法";
        }
    }

    static class Nested {
        String describe(NestedClassDemo outer) {
            return outer.name + " / " + school;
        }
        // String wrong() { return name; } // 没有外部对象，不能编译
    }

    public static void main(String[] args) {
        NestedClassDemo outer = new NestedClassDemo("张三");
        NestedClassDemo.Inner inner = outer.new Inner();
        NestedClassDemo.Nested nested = new NestedClassDemo.Nested();

        System.out.println(inner.describe());
        System.out.println(nested.describe(outer));
        System.out.println(NestedClassDemo.Inner.label());
    }
}
```

输出：

```text
张三 / Java课堂
张三 / Java课堂
Inner 的静态方法
```

“非静态内部类不能定义静态成员”这条限制需要按版本区分：Java 16 以前通常只能声明常量变量这样的静态成员；Java 16 起允许静态成员和静态初始化器。它仍然需要外部实例来创建普通 Inner 对象。[JLS 8.1.3 内部类](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.3)

静态嵌套类自己也可以有实例字段、实例方法，是否 static 描述的是它与外部类的关系，不是要求它内部所有成员都必须静态。

## 五、泛型

泛型把类型作为参数写进类、接口或方法。例如 `List<Emp>` 表示元素类型是 Emp 的列表。编译器因此可以检查添加、取出和赋值操作，并让同一段代码处理不同类型。

### 1. List<Emp> 与里面的 Emp

> **几句话看懂：** List<Emp> 是员工列表，get 取出的才是一个 Emp。列表管理元素，员工对象的方法修改员工自己的状态。

List 是列表接口，不是数组。`List<Emp>` 是一个存放 Emp 对象引用的集合；`list.get(0)` 才取出其中一个 Emp。列表的方法和员工的方法，调用对象不同。

```java
import java.util.ArrayList;
import java.util.List;

public class GenericListDemo {
    static class Emp {
        private String name;
        private int age;

        Emp(String name, int age) {
            this.name = name;
            this.age = age;
        }
        void setAge(int age) {
            this.age = age;
        }
        String describe() {
            return name + "，" + age + "岁";
        }
    }

    public static void main(String[] args) {
        List<Emp> employees = new ArrayList<>();
        employees.add(new Emp("张三", 20));
        // employees.add("李四"); // String 不是 Emp，不能编译

        System.out.println(employees.size());
        Emp first = employees.get(0);
        first.setAge(21);
        System.out.println(employees.get(0).describe());
        // employees.setAge(21); // 列表没有员工的 setAge 方法

        List<Object> mixed = new ArrayList<>();
        mixed.add(Integer.valueOf(100));
        mixed.add("100");
        System.out.println(mixed.get(0).getClass().getSimpleName());
        System.out.println(mixed.get(1).getClass().getSimpleName());
    }
}
```

输出：

```text
1
张三，21岁
Integer
String
```

`first` 与 `employees.get(0)` 指向同一个 Emp，所以通过 first 修改年龄后，再从列表取出也能看到修改。泛型约束元素类型，不会自动复制对象。

`List<Object>` 允许多种对象，是显式选择了宽泛的类型。省略类型参数写成原始类型 `List`，则会失去部分编译期检查，读取后也容易出现类型转换错误。

泛型实参不能直接是基本类型：使用 `List<Integer>`，不能写 `List<int>`。添加整数时可以自动装箱成 Integer。

### 2. 泛型类：同一个 Box 保存不同类型

> **几句话看懂：** T 是类型参数，使用 Box<String> 时按 String 检查，使用 Box<Integer> 时按 Integer 检查。这样同一个类可以复用，而不必丢掉类型约束。

T 是类型参数名，常用 T、E、K、V 表示不同角色。它不是固定的 Java 类型。使用 `Box<String>` 时，这个 Box 的 set 和 get 按 String 进行类型检查；使用 `Box<Integer>` 时按 Integer 检查。

```java
public class GenericBoxDemo {
    static class Box<T> {
        private T value;
        public void set(T value) {
            this.value = value;
        }
        public T get() {
            return value;
        }
    }

    public static void main(String[] args) {
        Box<String> textBox = new Box<>();
        textBox.set("Java笔记");
        String text = textBox.get();

        Box<Integer> numberBox = new Box<>();
        numberBox.set(100);
        Integer number = numberBox.get();

        // textBox.set(100); // String 类型的 Box 不能放 Integer
        System.out.println(text);
        System.out.println(number);
    }
}
```

输出：

```text
Java笔记
100
```

这样不用分别编写 StringBox、IntegerBox。`new Box<>()` 中的菱形语法让编译器根据上下文推断类型。

### 3. 泛型方法与泛型接口

> **几句话看懂：** 泛型方法在返回类型前声明自己的类型参数。泛型接口由实现类确定类型，让相同的接口结构处理不同数据。

方法自己的类型参数写在返回类型前，例如 `static <T> T first(...)`。它不要求整个类也使用 `<T>`。泛型接口则在实现时确定或继续传递类型参数。

```java
import java.util.List;
import java.util.Locale;

public class GenericMethodDemo {
    interface Formatter<T> { String format(T value); }

    static class TextFormatter implements Formatter<String> {
        @Override
        public String format(String value) {
            return value.toUpperCase(Locale.ROOT);
        }
    }

    public static <T> T first(List<T> values) {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("列表不能为空");
        }
        return values.get(0);
    }

    public static void main(String[] args) {
        String word = first(List.of("java", "spring"));
        Integer number = first(List.of(21, 22));
        Formatter<String> formatter = new TextFormatter();
        System.out.println(word);
        System.out.println(number);
        System.out.println(formatter.format(word));
    }
}
```

输出：

```text
java
21
JAVA
```

两次调用 first 使用同一段方法代码，但根据传入列表推断出不同类型。静态方法不能直接使用所在泛型类的类型参数；需要时，应像这里一样声明方法自己的类型参数。

泛型主要提供编译期类型检查。注意不要用原始类型 List 绕过这些检查；也不能在泛型类中直接写 new T()。需要动态创建对象时，要明确提供类型信息，[反射篇](#/articles/java-reflection-annotations)演示了取得构造方法后创建对象的过程。

## 官方资料

- [JLS 6.6 访问控制](https://docs.oracle.com/javase/specs/jls/se21/html/jls-6.html#jls-6.6)：访问修饰符与 protected 的跨包限制。
- [JLS 8 类与成员](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html)：final、static、内部类和嵌套类。
- [JLS 4 类型与变量](https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html)：类型参数、参数化类型与擦除。
