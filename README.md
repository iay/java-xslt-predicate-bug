# `java-xslt-predicate-bug`

This repository contains a replication of what I think is a bug
in Java's handling of XPath expressions in the context of
XSLT templates.

Here's the document we're analysing:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<doc>
<item entityID='http://aaaa'>starts with http</item>"
<item entityID='urn:mace:glark'>starts with urn:mace:</item>"
<item entityID='xxx'>no predicate matches</item>"
</doc>
```

The XPath expression we evaluate against this document looks like
this:

```xpath
//item
   [not(starts-with(@entityID, 'urn:mace:'))]
   [not(starts-with(@entityID, 'http://'))]
   [not(starts-with(@entityID, 'https://'))]
```

I have broken this onto three lines for clarity. It means "find all the `<item>`
elements whose `@entityID` attribute does not start with any of the three
given constants".

The reproduction applies this expression in two ways:

- As an XPath expression via the JDK's `XPathExpression` API. This correctly
  detects that only the third `<item>` element matches.
- As the selection criterion in an XSLT template, using the JDK's
  `Transformer` API. The result should be the same, but in fact
  more than one of the `<item>` elements are matched. Even more
  peculiarly, if you reorder the three predicates you will get
  different results; this should not happen either.

This behaviour is reminiscent of a bug reported in the Xalan-J XSL processor,
from which the processor in the JDK is descended.
The original Xalan issue is
[XALANJ-1434](https://issues.apache.org/jira/browse/XALANJ-1434), dating to 2003
and fixed in 2007. This fix was apparently applied to the JDK: the
corresponding JDK issue is
[JDK-7133220](https://bugs.openjdk.org/browse/JDK-7133220), fixed in 2012 for Java 7.
This is obviously not (quite) that bug, as XPath operation is correct.

However, it seems this this *may* be a related issue: executing the *same*
replication test with the final release of Xalan-J (from 2014)
*in the classpath* shows that the XPath and XSLT cases match the same
elements in the document.

The `./test` script does the following:

- Compiles the example using whichever Java compiler is in the path.
- Executes the test under the JDK.
- Executes the test again with Xerces-J/Xalan-J in the classpath.

Example output:

```text
java version "11.0.10" 2021-01-19 LTS
Java(TM) SE Runtime Environment 18.9 (build 11.0.10+8-LTS-162)
Java HotSpot(TM) 64-Bit Server VM 18.9 (build 11.0.10+8-LTS-162, mixed mode)

********** TESTING JDK **********

Performing XPath test.
Nodes found: 1 (expected: 1)
Node 0: no predicate matches entityID="xxx"

Performing XSLT test.
Nodes: 4
Elements: 2 (expected: 1)
Element: problem: http://aaaa: starts with http
Element: problem: xxx: no predicate matches


********** TESTING Xalan **********

Performing XPath test.
Nodes found: 1 (expected: 1)
Node 0: no predicate matches entityID="xxx"

Performing XSLT test.
Nodes: 7
Elements: 1 (expected: 1)
Element: problem: xxx: no predicate matches
```

To me, this is suggestive that there is a *second* issue, similar to XALANJ-1434,
which affected XSLT processing rather than XPath. It was fixed in Xalan-J, but
that fix never made it into the JDK.

There's a workround. The expression `x[a][b][c]` can also be expressed
as `x[a AND b AND c]`. That gives the correct answer in both cases,
as far as I can tell. It's quite a disruptive change to an old XSLT
codebase, however, so it's far from ideal.
