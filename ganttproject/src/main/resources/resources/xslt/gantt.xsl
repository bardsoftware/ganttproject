<?xml version="1.0" encoding="iso-8859-2" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>

<xsl:include href="gantt-utils.xsl"/>

<xsl:template match="project">
    <table width="550" border="0">
     <tr>
      <td valign="top" bgcolor="#dddddd"><h5><xsl:value-of select="name/@title"/></h5></td>
      <td valign="top"><h6><xsl:value-of select="name"/></h6></td>
     </tr>
     <tr>
      <td valign="top" bgcolor="#dddddd"><h5><xsl:value-of select="organization/@title"/></h5></td>
      <td valign="top"><h6><xsl:value-of select="organization"/></h6></td>
     </tr>
     <tr>
      <td valign="top" bgcolor="#dddddd"><h5><xsl:value-of select="webLink/@title"/></h5></td>
      <td valign="top"><a><xsl:attribute name="href"><xsl:value-of select="webLink"/></xsl:attribute><h6><xsl:value-of select="webLink"/></h6></a></td>
     </tr>	
     <tr>
      <td valign="top" bgcolor="#dddddd"><h5><xsl:value-of select="description/@title"/></h5></td>
      <td valign="top"><h6><xsl:value-of select="description"/></h6></td>
     </tr>
    </table>
</xsl:template>

</xsl:stylesheet>


