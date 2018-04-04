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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import com.ibm.es.ama.zing.common.AmaIngestionException;
import com.ibm.es.ama.zing.common.model.axml.AXMLBase.Publisher;
import com.ibm.es.ama.zing.common.model.axml.Content;
import com.ibm.es.ama.zing.common.model.axml.CrawlData;
import com.ibm.es.ama.zing.common.model.axml.CrawlData.Builder;
import com.ibm.es.ama.zing.common.model.converter.ConverterBase;
import com.ibm.es.ama.zing.common.model.converter.CustomConverterPlugin;
import com.ibm.es.ama.zing.common.model.axml.Document;

public class CsvPlugin implements CustomConverterPlugin {

    @Override
    public boolean canProcess(CrawlData data) {
        if (getGeneralSettings() != null) {
            return CustomConverterPlugin.super.canProcess(data);
        } else {
            return "text/csv".equals(data.getContentType());
        }
    }

    @Override
    public boolean canReadMetadataFrom(CrawlData data) {
        if (getGeneralSettings() != null) {
            return CustomConverterPlugin.super.canReadMetadataFrom(data);
        } else {
            return "application/metadata".equals(data.getContentType());
        }
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
        ConverterStatus status = ConverterBase.ConverterStatus.NO_OUTPUT;
        ArrayList<Content> metaContents = new ArrayList<>();
        for (Content content : document) {
            if (!content.getName().equals("body")) {
                metaContents.add(content);
            }
        }
        for (Document metaDocument : metaDocumentList) {
            for (Content content : metaDocument) {
                metaContents.add(content);
            }
        }
        for (Content content : document) {
            if (content.getName().equals("body")) {
                try (BufferedReader reader = new BufferedReader(content.getValueAsReader(true))) {
                    long row = 0;
                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        Document.Builder documentBuilder = document.builder();
                        int columnNumber = 0;
                        for (String column : line.split(",")) {
                            Content.Builder contentBuilder = content.builder();
                            contentBuilder.setName("@" + columnNumber++);
                            contentBuilder.setValueFromString(column);
                            documentBuilder.append(contentBuilder.get());
                        }
                        for (Content metaContent : metaContents) {
                            documentBuilder.append(metaContent);
                        }
                        documentBuilder.setGroupID(document.getGroupID() + "?row=" + row);
                        documentBuilder.setUrl(document.getUrl() + "?row=" + row++);
                        documentPublisher.append(documentBuilder.get());
                        status = ConverterBase.ConverterStatus.MODIFIED;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return status;
    }
}