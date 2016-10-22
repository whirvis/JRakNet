# JRakNet code standards
This file contains the standard syntax JRakNet uses to keep code clean and consistent.

# Variable and function references
When referencing variables that are specific to the class only, always use:

```java
this.variable
```

However, if the variable is being used to call a function or another variable we would then just use
```java
variable.anotherVariable
// or
variable.anotherFunction()
```

# Condition (If/White/For) statements
When using condition statements, if the variable being referenced to is a native type (Or an enum which can be considered
a synthetic native type) always use:

```java
if(this.nativeType == nativeValue) {
     // result
}
// or
if(ENUM.ENUMTYPE == ENUMTYPE) {
    // result
}
```

While the example did use curly braces, if the if condition statement uses only one line of code simply use:

```java
if(this.nativeType == nativeValue)
    System.out.println("Look ma, no curly braces!);
```

However, still use curly braces for all of them if the if, else if, or else used curly braces. An example of this would be:

```java
// Incorrect
if(this.nativeType == nativeValue)
    System.out.println("Look ma, no curly braces!");
else {
    System.err.println("Fatal error, not the nativeValue we wanted!");
    System.exit(1);
}

// Correct
if(this.nativeType == nativeValue) {
    System.out.println("Look ma, no curly braces!");
} else {
    System.err.println("Fatal error, not the nativeValue we wanted!");
    System.exit(1);
}

```

For if statements using functions, if the function returns a boolean either just put in the function or use ```!```
to negate it if you are checking for false from the function. An example of this would be:

```java
int functionThatReturnsAnInt() {
    return 1;
}

boolean functionThatReturnsABoolean() {
    return true;
}

void functionThatChecksTheOtherFunctions() {
    if(funtionThatReturnsAnInt() == 1)
        System.out.println("It returns 1, yay!");
    else
        System.out.println("It doesn't return 1, boo!");
    
    // Normally here you would just use an else statement, but I needed to have an example
    if(functionThatReturnsABoolean())
        System.out.println("It returns true, yay!");
    else if(!functionThatReturnsABoolean())
        System.out.println("It returns false, boo!");
}
```

However, if one if statement uses ```==``` then all of them should. An example of this would be:
```java
int functionThatReturnsAnInt() {
    return 1;
}

boolean functionThatReturnsABoolean() {
    return true;
}

// Incorrect
if(functionThatReturnsAnInt() == 1)
    System.out.println("It returns 1, yay!");
else if(functionThatReturnsABoolean())
    System.out.println("It returns true, yay!");

// Correct
if(functionThatReturnsAnInt() == 1)
    System.out.println("It returns 1, yay!");
else if(functionThatReturnsABoolean() == true)
    System.out.println("It returns true, yay!");
```

# Enumerators (enums)
All enums must have more than one value (or plan to have more than one value) otherwise they will be considered useless.
On top of this, their values must also always be in uppercase and end in a semicolon as if it has a constructor even if it doesn't
have one and it is just optional. An example of this would be:

```java

enum TestEnumOne {

    VALUE_ONE(0), VALUE_TWO(1);
    
    private int value;
    
    private TestEnumOne(int value) {
        this.value = value;
    }

}

// or

enum TestEnumTwo {
    
    VALUE_ONE, VALUE_TWO;
    
}
```

While the values are in all-caps and underscore for spaces, the enum class names still follow the generic class naming rules.

# Final variables
All final variables must follow the rules of values in the enums, all-caps and underscores for spaces:

```java
public static final long MILLI_IN_ONE_SECOND = 1000L;
```

#Longs, Floats, and Doubles
All floats, doubles, and longs must end in their exclusive signal even though it is optional in the language:

```java
public static final float EXAMPLE_FLOAT = 1.0F;
public static final double EXAMPLE_DOUBLE = 1.0F;
public static final long EXAMPLE_LONG = 1L;
```

# Documentation
Every single class and method must be documented for the javadoc even if it is obvious through their name.
Every class must have their @author set. For methods, they must have their @param names and their use set and
all of the @throws listed and why they would be thrown (Constructors follow the same rules). An example of this would be:

```java
/**
* This is a documented class, it is used to give an example of what documentation would look like
* in JRakNet and MarfGamers other projects.
*
* @author MarfGamer
*/
class DocumentedClass {

    /**
    * This is a documented function, which makes sure the documented string is not null
    *
    * @param documentedString
    *            - The documented String that will be checked for a null value
    * @throws DocumentedException
    *            - Thrown when the documented String is null
    */
    public void documentedFunction(String documentedString) throws DocumentedException {
        if(documentedString == null) {
            throw new DocumentedException("The documented String was null!");
        }
    }

}
```

#Comments
Single lines comments use ```//```, while multiline comments must use ```/* */```. A correct example of this would be:
```java
// This is correct, as this uses only one line to comment

/*
 * This is incorrect, as it uses only one line to comment
 */
 
/*
 * This is correct, as I use not only one line to comment.
 * I use this second line to prove that the first one is true.
 */
 
 // This is incorrect, as I use not only one line to comment
 // I use this second line to prove that the first one is true.
```
