/*******************************************************************************
* Copyright (c) 2018 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*******************************************************************************/

package plugins.custom;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.ibm.es.ama.zing.common.AmaIngestionException;
import com.ibm.es.ama.zing.common.model.axml.AXMLBase.Publisher;
import com.ibm.es.ama.zing.common.model.axml.CrawlData.Builder;
import com.ibm.es.ama.zing.common.model.converter.CustomConverterPlugin;
import com.ibm.es.ama.zing.common.model.axml.Content;
import com.ibm.es.ama.zing.common.model.axml.CrawlData;
import com.ibm.es.ama.zing.common.model.axml.Document;

public class XMLConverter implements CustomConverterPlugin {

    private static final String ELEMENT_FIELD = "element-name";
    private static final String BODY_FIELD = "body";
    private static final String ATTRIBUTE_PREFIX = "@";

    private boolean rootElementAsDocument = false;

    @Override
    public void configure(Map<String, String> parameters) {
        rootElementAsDocument = Boolean.parseBoolean(parameters.get("rootElementAsDocument"));
    }

    @Override
    public boolean canProcess(CrawlData data) {
        if (getGeneralSettings() != null) {
            return CustomConverterPlugin.super.canProcess(data);
        } else {
            return "text/xml".equals(data.getContentType());
        }
    }

    @Override
    public boolean canReadMetadataFrom(CrawlData data) {
        return "application/metadata".equals(data.getContentType());
    }

    @Override
    public boolean removesMetadata() {
        return true;
    }

    @Override
    public void postProcess(CrawlData data, Builder builder) {
        if (getGeneralSettings() != null) {
            CustomConverterPlugin.super.postProcess(data, builder);
        } else {
            builder.setContentType("application/axml");
        }
    }

    @Override
    public boolean forcesDeletion() {
        return true;
    }

    @Override
    public ConverterStatus convert(Document document, Publisher<? super Document> documentPublisher,
            Iterable<Document> metaDocumentList) throws AmaIngestionException {

        ConverterStatus status = ConverterStatus.NO_CHANGE;

        List<Document.Builder> docBuilderList = new ArrayList<>();
        List<Content> remainingContentList = new ArrayList<>();
        for (Content content : document) {
            if (content.getName().equals("body")) {
                try {

                    SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
                    parser.parse(new InputSource(content.getValueAsReader(true)), new DefaultHandler() {

                        Document.Builder documentBuilder;
                        Stack<StringBuilder> contentStack = new Stack<StringBuilder>();
                        boolean isSeparated = false;
                        long row = 0;

                        private boolean isRoot() {
                            return rootElementAsDocument ? false : contentStack.size() == 0;
                        }

                        private boolean isDocumentElement() {
                            return rootElementAsDocument ? contentStack.size() == 0 : contentStack.size() == 1;
                        }

                        @Override
                        public void startElement(String uri, String localName, String qName, Attributes attributes) {
                            if (isRoot()) {
                                // ignore root document?
                            } else if (isDocumentElement()) {
                                documentBuilder = document.builder();
                                Content.Builder contentBuilder = content.builder();
                                contentBuilder.setName(ELEMENT_FIELD);
                                contentBuilder.setValueFromString(qName);
                                documentBuilder.append(contentBuilder.get());
                                for (int k = 0; k < attributes.getLength(); k++) {
                                    contentBuilder = content.builder();
                                    contentBuilder.setName(ATTRIBUTE_PREFIX + attributes.getQName(k));
                                    contentBuilder.setValueFromString(attributes.getValue(k));
                                    documentBuilder.append(contentBuilder.get());
                                }
                            } else {
                                for (int k = 0; k < attributes.getLength(); k++) {
                                    Content.Builder contentBuilder = content.builder();
                                    contentBuilder.setName(qName + ATTRIBUTE_PREFIX + attributes.getQName(k));
                                    contentBuilder.setValueFromString(attributes.getValue(k));
                                    documentBuilder.append(contentBuilder.get());
                                }
                            }
                            contentStack.push(new StringBuilder());
                            isSeparated = false;
                        }

                        @Override
                        public void endElement(String uri, String localName, String qName) {
                            String contentValue = contentStack.pop().toString().trim();
                            if (!contentValue.isEmpty()) {
                                if (isRoot()) {
                                    // ignore root document?
                                } else if (isDocumentElement()) {
                                    Content.Builder contentBuilder = content.builder();
                                    contentBuilder.setName(BODY_FIELD);
                                    contentBuilder.setValueFromString(contentValue);
                                    documentBuilder.append(contentBuilder.get());
                                } else {
                                    Content.Builder contentBuilder = content.builder();
                                    contentBuilder.setName(qName);
                                    contentBuilder.setValueFromString(contentValue);
                                    documentBuilder.append(contentBuilder.get());
                                }
                            }
                            if (isDocumentElement()) {
                                for (Document metaDocument : metaDocumentList) {
                                    documentBuilder.appendAll(metaDocument);
                                }
                                documentBuilder.setGroupID(document.getGroupID() + "?row=" + row);
                                documentBuilder.setUrl(document.getUrl() + "?row=" + row++);
                                documentPublisher.append(documentBuilder.get());
                            }
                            isSeparated = true;
                        }

                        @Override
                        public void characters(char[] ch, int start, int length) throws SAXException {
                            if (length == 0)
                                return;
                            if (isSeparated) {
                                contentStack.peek().append(' ');
                            }
                            contentStack.peek().append(ch, start, length);
                            isSeparated = false;
                        }

                    });

                    status = ConverterStatus.MODIFIED;
                } catch (Exception e) {
                    remainingContentList.add(content);
                }
            } else {
                remainingContentList.add(content);
            }
        }
        if (status == ConverterStatus.MODIFIED) {
            for (Document.Builder docBuilder : docBuilderList) {
                docBuilder.appendAll(remainingContentList);
                documentPublisher.append(docBuilder.get());
            }
        } else {
            documentPublisher.append(document);
        }

        return status;
    }

}
