# OneWEX Custom Converter Samples and API Javadoc

## Sample code

To build the following java projects, you need `ama-commons-<version>.jar`, `ama-zing-commons-<version>.jar` and `commons-io-2.5.jar` in your classpath. 

* [Simple csv converter](src/plugins/custom/CsvPlugin.java): An example csv converter, which turns each row to a separate document, with its contents built from a file with comma-separated columns.
* [XML converter](src/plugins/custom/XMLConverter.java): An example XML converter.  This converter can take a parameter `rootElementAsDocument`.  If the value is true, single XML is converted into single document. If false, each child element of the root in a single input XML is turned to a separate document.  

## API javadoc

The sample code uses the  custom converter API under packages `com.ibm.es.ama.zing.common.model.{axml,converter}`.  You can find the javadoc at [https://watson-explorer.github.io/onewex-custom-converters/javadoc/](https://watson-explorer.github.io/onewex-custom-converters/javadoc/).

## Testing the code

Once you build the code, you can try the conversion inside your converter pipeline by specifying the jar location and plugin class name. There is also a [command-line runner](https://watson-explorer.github.io/onewex-custom-converters/javadoc/com/ibm/es/ama/zing/common/model/converter/CustomConverterPlugin.html#main-java.lang.String:A-) to test the converter.  
