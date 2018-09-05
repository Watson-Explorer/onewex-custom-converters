# IBM Watson Explorer oneWEX Custom Converter Resources

- [What's this](#what)
- [Custom converters using XSLT](#xslt)
- [Custom converters using Java plugin](#java)

## What's this <a name="what"></a>

A converter pipeline in IBM Watson Explorer oneWEX is a phase during
dataset ingestion, that manipulates the raw data fetched by crawlers,
and transforms it into dataset documents consisting of text (body)
fields, as well as various metadata fields.  In order to work with
varying enterprise data sources, in addition to built-in converters
for common content types, we also provide options to use _custom
converters_ inside the converter pipeline.

When using custom converters, we have two implementation language options:
XSLT and Java. 

Prerequisite: To try out the XSLT sample code, you need the version of
oneWEX installation higher than 12.0.1.  You can only use java-based
converters in oneWEX 12.0.0.


## Custom converters using XSLT <a name="xslt"></a>

The option to use oneWEX's XSLT-based custom converters is suitable
for converting either XML (`text/xml`, `application/xml`) or HTML
(`text/html`) data.  To process other types of data, you either first
process them using built-in converters, or consider using Java-based
custom converters.

### Sample XML-to-AXML converter

Let's get started with an example. 

1. Place the following file (`custom-xml.xsl`) at the mounted
directory seen from docker or ICP instances, e.g., as
`/mnt/custom-xsl/custom-xml.xsl`.

* [XML converter](src/custom-xsl/custom-xml.xsl): An example XML
  converter.  This converter can take a parameter
  `root-element-as-document`.  If the value is true, single XML is
  converted into single document. If false, each child element of the
  root in a single input XML is turned to a separate document.

2. Create a dataset from admin console, with a crawler of your choice.
At converter pipeline section, add a custom converter, with the following
configuration. 

* Content-types for input: `text/xml`
* Content-types for metadata: `application/metadata`
* Content-types for output: `application/axml`
* Converter type: `Converting XML docuemnts using xsl templates` 
* Converter template path: `/mnt/custom-xsl/custom-xml.xsl`
* Config patameters: press Add, and edit the fields by key : `root-element-as-document`,
and value : `false`. 

3. Press OK, and move the custom converter up just before the document filter converter.
Then save the converter pipeline. 

4. At Test -> Upload, select a xml file in your directory, e.g.,
which may look like the following example, and then Start. 
```
<?xml version="1.0" encoding="UTF-8"?>
<docs>
   <doc>
      <id>00000000</id>
      <title>lemon tea - Package / container</title>
      <date>2008-01-01</date>
      <timestamp>1199186392296</timestamp>
      <category>Package / container</category>
      <subcategory>Straw</subcategory>
      <product>lemon tea</product>
      <text>[Pack] The straw was peeled off from the juice pack.</text>
   </doc>
</docs>
``` 

### Result

We get the result that looks as follows.
```
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<crawl-url crawlspace-id="3dbf7d7d-b9d2-1180-0000-01616f56e452-testit-target" crawler-id="3dbf7d7d-b9d2-1180-0000-01616f56e452-testit" enqueueEnabled="false" force-deletion="true" hops="0" test-it="true" group-id="file:///wexdata/zing/data/dataset/3dbf7d7d-b9d2-1180-0000-01616f56e452/testit/target/00000000.xml" url="file:///wexdata/zing/data/dataset/3dbf7d7d-b9d2-1180-0000-01616f56e452/testit/target/00000000.xml">
    <crawl-data content-type="application/axml" group-id="file:///wexdata/zing/data/dataset/3dbf7d7d-b9d2-1180-0000-01616f56e452/testit/target/00000000.xml" url="file:///wexdata/zing/data/dataset/3dbf7d7d-b9d2-1180-0000-01616f56e452/testit/target/00000000.xml">
        <document size="379">
            <content flags="" name="element-name">doc</content>
            <content flags="" name="id">00000000</content>
            <content flags="" name="title">lemon tea - Package / container</content>
            <content flags="" name="date">2008-01-01</content>
            <content flags="" name="timestamp">1199186392296</content>
            <content flags="" name="category">Package / container</content>
            <content flags="" name="subcategory">Straw</content>
            <content flags="" name="product">lemon tea</content>
            <content flags="" name="text">[Pack] The straw was peeled off from the juice pack.</content>
        </document>
    </crawl-data>
</crawl-url>
```

The output of oneWEX custom converter should be of content type
`application/axml`. The output of conversion is enclosed in a crawl-data
element, and is either a single or list of `document` elements, each
containing `content` elements which represents a set of  fields inside the document.

### Group-ids and virtual documents 

The oneWEX converter pipeline supports virtual (compound) documents.
Inside a dataset, a group of documents with the same group-id are
joined together,  and forms a single document.  

On the other hand, if the converter needs to create multiple
separate documents,  we need to assign unique group-id to each
document manually.  The following code snippet achieves this
job in [custom-xml.xsl](src/custom-xsl/custom-xml.xsl). 

```
    <xsl:variable name="docnum">
      <xsl:number />
    </xsl:variable>
    <document url="{concat(ama:documentUrl(), '?row=', $docnum)}"
      group-id="{concat(ama:documentGroupId(), '?row=', $docnum)}">
      ...
    </document>
```
If we create a document without specifying group-id, the default behavior
is to inherit the value from the enclosing crawl-data.  

You can check that feeding xml data containing two `doc` elements, results in
document elements with distinct group-ids. 



### oneWEX XSL extension functions 

In the example above, we see functions with `ama:` prefix appearing inside XPath expressions (
`ama:documentUrl()`, `ama:groupId()`).  These functions are oneWEX
XSL extension functions, that are provided for various purposes, e.g., utility functions not
available in built-in/common extension functions, communication with oneWEX runtime, etc. 

The oneWEX XSL extension functions are implemented as Java methods,
which are called from XPath expressions using Xalan XSLT extension
function mechanism.  The current list of supported extensions is
available as Javadoc here:
[https://watson-explorer.github.io/onewex-custom-converters/ama-extensions/com/ibm/es/ama/zing/converter/converters/xslt/AmaExtensions.html](https://watson-explorer.github.io/onewex-custom-converters/ama-extensions/com/ibm/es/ama/zing/converter/converters/xslt/AmaExtensions.html).

In addition to oneWEX extension functions, we can use standard [EXSLT extenison functions](http://exslt.org)
available in the Xalan XSLT processor.  

## Custom converters using Java plugin <a name="java"></a>

### Sample code

To build the following java projects, you need
`ama-commons-<version>.jar`, `ama-zing-commons-<version>.jar` and
`commons-io-2.5.jar` in your classpath.

* [Simple csv converter](src/plugins/custom/CsvPlugin.java): An
  example csv converter, which turns each row to a separate document,
  with its contents built from a file with comma-separated columns.
* [Java-based XML converter](src/plugins/custom/XMLConverter.java):
  This converter does exactly the same conversion as the previous
  `custom-xml.xsl` example using XSLT.  The Java version uses SAX
  parser to process XML documents.

### API javadoc

The sample code uses the custom converter API under packages
`com.ibm.es.ama.zing.common.model.{axml,converter}`.  You can find the
javadoc at
[https://watson-explorer.github.io/onewex-custom-converters/javadoc/](https://watson-explorer.github.io/onewex-custom-converters/javadoc/).

### Testing the code

Once you build the code, you can try the conversion inside your
converter pipeline by specifying the jar location and plugin class
name. There is also a [command-line
runner](https://watson-explorer.github.io/onewex-custom-converters/javadoc/com/ibm/es/ama/zing/common/model/converter/CustomConverterPlugin.html#main-java.lang.String:A-)
to test the converter.


