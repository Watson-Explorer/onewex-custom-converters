<?xml version="1.0" encoding="utf-8"?>
<!-- 
 Copyright (c) 2018 IBM Corp.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan"
  xmlns:ama="xalan://com.ibm.es.ama.zing.converter.converters.xslt.AmaExtensions"
  extension-element-prefixes="xalan ama">

  <xsl:param name="root-element-as-document" select="'false'" />

  <xsl:strip-space elements="*" />

  <xsl:template match="/">
    <xsl:choose>
      <xsl:when test="$root-element-as-document and $root-element-as-document!='false'">
        <xsl:apply-templates select="/*" mode="document-element" />
      </xsl:when>
      <xsl:otherwise>
        <documentList>
          <xsl:apply-templates select="/*/*" mode="document-element" />
        </documentList>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*" mode="document-element">
    <xsl:variable name="docnum">
      <xsl:number />
    </xsl:variable>
    <document url="{concat(ama:documentUrl(), '?row=', $docnum)}"
      group-id="{concat(ama:documentGroupId(), '?row=', $docnum)}">
      <content name="element-name">
        <xsl:value-of select="name()" />
      </content>
      <xsl:for-each select="@*">
        <content name="{concat('@', name())}">
          <xsl:value-of select="@*" />
        </content>
      </xsl:for-each>
      <xsl:if test="text()">
        <content name="body">
          <xsl:value-of select="text()" />
        </content>
      </xsl:if>
      <xsl:apply-templates select="*" mode="content-element" />
    </document>
  </xsl:template>

  <xsl:template match="*" mode="content-element">
    <xsl:for-each select="@*">
      <content name="{concat('@', name())}">
        <xsl:value-of select="@*" />
      </content>
    </xsl:for-each>
    <content name="{name()}">
      <xsl:value-of select="text()" />
    </content>
    <xsl:apply-templates select="*" mode="content-element" />
  </xsl:template>

</xsl:stylesheet>
