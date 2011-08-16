<?xml version="1.0" encoding="iso-8859-2" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ganttproject="http://ganttproject.sf.net/" version="1.0">

<xsl:output method="html" indent="yes" encoding="UTF-8"/>

<xsl:template match="/">
<html>
    <head>
    <title><xsl:value-of select="/ganttproject/title"/></title>
    <meta http-equiv="content-type" content="text/html; charset=ISO-8859-2" />
    <meta name="author" content="Pawel Lipinski" />
    <link href="css/styles.css" rel="stylesheet" type="text/css" />
    <style type="text/css">
	    a {
		    text-decoration: none;
		    font-family: Arial;
		    color: rgb(255, 255, 255);
		    border: none;
	    }
	    a:hover {
		    text-decoration: none;
		    font-family: Arial;
		    color: #dc2dff;
		    border: none;
	    }
	    body {
		    margin: 0;
	    }
    </style>
  </head>

  <body>
    <div align="center">
      <table cellpadding="0" cellspacing="0" border="0" width="990" height="620">
        <tr>
          <td valign="top" colspan="2" background="images/top.jpg" width="990" height="94">
            <br />
          </td>
        </tr>

        <tr>
	    <td valign="top" width="136">
	    <table width="100%" height="100%" cellspacing="0" cellpadding="0" border="0" background="images/menu.jpg">
                <tr>
                  <td height="50">
                    <br />
                  </td>
		  <td width="5">
                    <br />
                  </td>
                </tr>

                <tr>
                  <td align="right" valign="top">
		    <a target="inner" id="chart">
			<xsl:attribute name="href"><xsl:value-of select="/ganttproject/links/@prefix" />-chart.html</xsl:attribute>
			<xsl:value-of select="/ganttproject/links/chart" />
		    </a>
                  </td>
                  <td>
                    <br />
                  </td>
                </tr>

                <tr>
                  <td align="right" valign="top">
		    <a target="inner" id="tasks">
			<xsl:attribute name="href"><xsl:value-of select="/ganttproject/links/@prefix" />-tasks.html</xsl:attribute>
			<xsl:value-of select="/ganttproject/links/tasks" />
		    </a>
                  </td>
                  <td>
                    <br />
                  </td>
                </tr>

                <tr>
                  <td align="right" valign="top">
		    <a target="inner" id="resources">
			<xsl:attribute name="href"><xsl:value-of select="/ganttproject/links/@prefix" />-resources.html</xsl:attribute>
			<xsl:value-of select="/ganttproject/links/resources" />
		    </a>
                  </td>
                  <td>
                    <br />
                  </td>
                </tr>

                <tr>
                  <td height="300" valign="bottom">
                    <br />
                  </td>
		  <td>
                    <br />
                  </td>
                </tr>
            </table>
          </td>

          <td valign="top" width="854" height="100%">
            <iframe frameborder="0" height="100%" id="inner" name="inner" width="100%">
		<xsl:attribute name="src"><xsl:value-of select="/ganttproject/links/@prefix" />-chart.html</xsl:attribute>
	    </iframe>
          </td>
        </tr>
      </table>
    </div>
  </body>
</html>
</xsl:template>

</xsl:stylesheet>
