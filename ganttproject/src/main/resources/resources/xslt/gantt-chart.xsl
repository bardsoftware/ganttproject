<?xml version="1.0" encoding="iso-8859-2" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>

<xsl:include href="gantt-utils.xsl"/>

<xsl:template match="chart">
<br/>
<img>
	<xsl:attribute name="src"><xsl:value-of select="."/></xsl:attribute>
</img>
<br/>
</xsl:template>

<xsl:template match="project" />
</xsl:stylesheet>
