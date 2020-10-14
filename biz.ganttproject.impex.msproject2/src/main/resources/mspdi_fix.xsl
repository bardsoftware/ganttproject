<?xml version="1.0" encoding="iso-8859-2" ?>
<!-- 
This XSLT fixes projects created by Planta Project, where <Project> element has no associated namespace.
See http://code.google.com/p/ganttproject/issues/detail?id=438
 -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:template match="/Project">
  <xsl:element name="Project" xmlns="http://schemas.microsoft.com/project">
  <xsl:apply-templates/>
  </xsl:element>
</xsl:template>

<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates/>
  </xsl:copy>
</xsl:template>
</xsl:stylesheet>