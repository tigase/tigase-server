Tigase Code Style
=====================

Introduction
-------------


This documents defines and describes coding style and standard used in Tigase projects source code.

Examples should be considered as **non-normative**, that is formatting choices should not be treated as rules.

Source file basics
----------------------

Technicals details
^^^^^^^^^^^^^^^^^^^^^^^

-  File name consists of the case-sensitive, camel-cased name of the top-level class it contains plus the ``.java`` extension.

-  Source files are encoded in **UTF-8**.


Source file structure
--------------------------

A source file consists of, **in order**:

1. License or copyright information, if present

2. Package statement

3. Import statements

4. Exactly one top-level class

Additionally:

-  **Exactly one blank line** separates sections 2-4;

-  The package statement is **not line-wrapped** (column limit does not apply);

Import statements
^^^^^^^^^^^^^^^^^^^^^^^

-  Wildcard imports can be used for:

   -  more than 5 class imports;

   -  more than 3 name imports;

-  import statements are **not line-wrapped** (column limit does not apply);

-  following import ordering applies:

   -  all imports not pertaining to any of the groups listed below

   -  ``blank line``

   -  ``javax.*`` classes

   -  ``java.*`` classes

   -  ``blank line``

   -  all static imports in single block

-  items in each block are ordered by names in ASCII sort order (since ``;`` sorts before ``.``)

Class declaration
^^^^^^^^^^^^^^^^^^^^^^^

-  Each top-level class resides in a source file of its own.

Class contents order
~~~~~~~~~~~~~~~~~~~~

Following order of the elements of the class is mandatory:

-  ``final``, ``static`` fields in following order:

   -  ``public``

   -  ``protected``

   -  ``package-private``

   -  ``private``

-  ``public`` ``enum``

-  ``static`` fields in following order:

   -  ``public``

   -  ``protected``

   -  ``package-private``

   -  ``private``

-  ``static`` initializer block

-  ``final`` fields in following order:

   -  ``public``

   -  ``protected``

   -  ``package-private``

   -  ``private``

-  fields without modifiers in following order:

   -  ``public``

   -  ``protected``

   -  ``package-private``

   -  ``private``

-  initializer block

-  ``static`` method(s)

-  constructor(s)

-  methods(s) without modifiers

-  enums(s) without modifiers

-  interfaces(s) without modifiers

-  inner ``static`` classes

-  inner classes

In addition:

-  Getters and Setters are kept together

-  Overloads are never split - multiple constructors or methods with the same name appear sequentially.

Formatting
-------------

Braces
^^^^^^^^

-  Braces are mandatory in optional cases - for all syntax where braces use can be optional, Tigase mandate using braces even if the body is empty or contains only single statement.

-  Braces follow the Kernighan and Ritchie style (`Egyptian brackets <http://www.codinghorror.com/blog/2012/07/new-programming-jargon.html>`__):

   -  No line break before the opening brace.

   -  Line break after the opening brace.

   -  Line break before the closing brace.

   -  Line break after the closing brace, *only if* that brace terminates a statement or terminates the body of a method, constructor, or *named* class. For example, there is *no* line break after the brace if it is followed by ``else`` or a comma.


Block indentation: tab
^^^^^^^^^^^^^^^^^^^^^^^^

All indentation (opening a new block of block-like construct) must be made with tabs. After the block, then indent returns to the previous.

Ideal tab-size: 4

Column limit: 120
^^^^^^^^^^^^^^^^^^^^^^^^

Defined column limit is 120 characters and must be line-wrapped as described below Java code has a column limit of 100 characters. Except as noted below, any line that would exceed this limit must be line-wrapped, as explained in section `Line-wrapping <#line-wrapping>`__.

Line-wrapping
^^^^^^^^^^^^^^^^

*line-wrapping* is a process of dividing long lines that would otherwise go over the defined Column Limit (above). It’s recommended to wrap lines whenever it’s possible even if they are not longer than defined limit.

Whitespace
^^^^^^^^^^^^^^^^

Vertical Whitespace
~~~~~~~~~~~~~~~~~~~~~~~

A single blank line appears:

-  after package statement;

-  before imports;

-  after imports;

-  around class;

-  after class header;

-  around field in interface;

-  around method in interface;

-  around method;

-  around initializer;

-  as required by other sections of this document.

Multiple blank lines are not permitted.

Horizontal whitespace
~~~~~~~~~~~~~~~~~~~~~~~

Beyond where required by the language or other style rules, and apart from literals, comments and Javadoc, a single ASCII space also appears in the following places **only**.

1. Separating any reserved word, such as ``if``, ``for``, ``while``, ``switch``, ``try``, ``catch`` or ``synchronized``, from an open parenthesis (``(``) that follows it on that line

2. Separating any reserved word, such as ``else`` or ``catch``, from a closing curly brace (``}``) that precedes it on that line

3. Before any open curly brace (``{``), with two exceptions:

   -  ``@SomeAnnotation({a, b})`` (no space is used)

   -  ``String[][] x = {{"foo"}};`` (no space is required between ``{{``, by item 8 below)

4. On both sides of any binary or ternary operator. This also applies to the following "operator-like" symbols:

   -  the ampersand in a conjunctive type bound: ``<T extends Foo & Bar>``

   -  the pipe for a catch block that handles multiple exceptions: ``catch (FooException | BarException e)``

   -  the colon (``:``) in an enhanced ``for`` ("foreach") statement

   -  the arrow in a lambda expression: ``(String str) → str.length()``

      **but not:**

   -  the two colons (``::``) of a method reference, which is written like ``Object::toString``

   -  the dot separator (``.``), which is written like ``object.toString()``

5. After ``,:;`` or the closing parenthesis (``)``) of a cast

6. Between the type and variable of a declaration: ``List<String> list``

Horizontal alignment: never required
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

*Horizontal alignment* is the practice of adding a variable number of additional spaces in your code with the goal of making certain tokens appear directly below certain other tokens on previous lines.

This practice is permitted, but is **never required**. It is not even required to *maintain* horizontal alignment in places where it was already used.

Specific constructs
^^^^^^^^^^^^^^^^^^^^

Enum classes
~~~~~~~~~~~~~~~~~~~~~~~

After each comma that follows an enum constant, a line break is mandatory.

Variable declarations
~~~~~~~~~~~~~~~~~~~~~~~

-  One variable per declaration - Every variable declaration (field or local) declares only one variable: declarations such as ``int a, b;`` are not used.

-  Declared when needed -Local variables are **not** habitually declared at the start of their containing block or block-like construct. Instead, local variables are declared close to the point they are first used (within reason), to minimize their scope. Local variable declarations typically have initializers, or are initialized immediately after declaration.

Arrays
~~~~~~~~~~~~~~~~~~~~~~~

Any array initializer may *optionally* be formatted as if it were a "block-like construct." (especially when line-wrapping need to be applied).

Naming
-----------------

Rules common to all identifiers
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Identifiers use only ASCII letters and digits, and, in a small number of cases noted below, underscores. Thus each valid identifier name is matched by the regular expression ``\w+`` .

Specific Rules by identifier type
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

-  Package names are all lowercase, with consecutive words simply concatenated together (no underscores, not camel-case).

-  Class names are written in **UpperCamelCase**.

-  Method names are written in **lowerCamelCase**.

-  Constant names use ``CONSTANT_CASE``: all uppercase letters, with words separated by underscores.

-  Non-constant field names (static or otherwise) are written in **lowerCamelCase**.

-  Parameter names are written in **lowerCamelCase** (one-character parameter names in public methods should be avoided).

-  Local variable names are written in **lowerCamelCase**.

Programming Practices
------------------------

-  A method is marked with the ``@Override`` annotation whenever it is legal. This includes a class method overriding a superclass method, a class method implementing an interface method, and an interface method re-specifying a super-interface method.

-  Caught exceptions should not be ignored (and if this is a must then a log entry is required).

Javadoc
----------

-  blank lines should be inserted after:

   -  description,

   -  parameter description,

   -  return tag;

-  empty tag should be included for following tags:

   -  ``@params``

   -  ``@return``

   -  ``@throws``

Usage
^^^^^^^

At the *minimum*, Javadoc is present for every ``public`` class, and every ``public`` or ``protected`` member of such a class, with a few exceptions:

-  is optional for "simple, obvious" methods like ``getFoo``, in cases where there *really and truly* is nothing else worthwhile to say but "Returns the foo".

-  in methods that overrides a supertype method.
