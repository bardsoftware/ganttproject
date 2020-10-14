<?xml version="1.0" encoding="iso-8859-2" ?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:ganttproject="http://ganttproject.sf.net/" version="1.0">

<xsl:template match="/">
    <html>
    <head>
	<meta http-equiv="content-type" content="text/html; charset=ISO-8859-2" />
	<link href="css/styles.css" rel="stylesheet" type="text/css" />
	<style type="text/css">
	    th {
			font-family: Tahoma,Arial;
			font-size: 12pt;
			font-weight: Bold;
			color: #002000;
			background-color: #FFFFFF;
	    }
	    td {
			font-family: Arial;
			font-size: 10pt;
			color: #00194c;
			text-align: center;
	    }
		a {
		    text-decoration: bold;
		    font-family: Arial;
		    color: rgb(0, 0, 0);
		    border: none;
	    }
	    a:hover {
		    text-decoration: bold;
		    font-family: Arial;
		    color: #dc2dff;
		    border: none;
	    }
	</style>
    </head>
    <body bgcolor="#e0ffb0" background="images/backg.png">
	<div align="center">
	    <xsl:apply-templates/>
	</div>		
    </body>
    </html>
</xsl:template>

<xsl:template match="links" />
<xsl:template match="title" />
<xsl:template match="project" />
<xsl:template match="chart" />
<xsl:template match="ganttproject:resources" />
<xsl:template match="ganttproject:tasks" />

</xsl:stylesheet>
