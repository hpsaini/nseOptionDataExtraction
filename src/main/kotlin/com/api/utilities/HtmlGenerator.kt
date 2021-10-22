package com.api.utilities

import com.api.constants.ProjectProperties
import java.io.File
import java.util.*

class HtmlGenerator {
    private var content: String = ""

    fun addTable(tableString: String): HtmlGenerator {
        content += tableString
        return this
    }

    /***
     * Comma separated values
     */
    fun addChart(xValues: String, yValues: String, chartHeading: String): HtmlGenerator {
        val canvasId = UUID.randomUUID().toString()
        content += """
        <canvas id="$canvasId" style="width:100%;max-width:600px"></canvas>

         <script>
         var xValues = [$xValues];
         var yValues = [$yValues];

         new Chart("$canvasId", {
           type: "line",
           data: {
             labels: xValues,
             datasets: [{
               fill: false,
               pointRadius: 1,
               borderColor: "rgba(255,0,0,0.5)",
               data: yValues
             }]
           },    
           options: {
             legend: {display: false},
             title: {
               display: true,
               text: "$chartHeading",
               fontSize: 16
             }
           }
         });

         </script>""".trimIndent()

        return this
    }

    fun build() {
        val OUTPUT_DIRECTORY: String = ProjectProperties.ROOT_JSON_FOLDER_PATH
        val fileName = "${Util().getFolderRootPath()}$OUTPUT_DIRECTORY/index.html"

        var file = File(fileName)

        // create a new file
        file.writeText(page())

    }

    private fun page(): String {
        return """
        <!DOCTYPE html>
        <html>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.5.0/Chart.min.js"></script>

         <meta http-equiv="refresh" content="5" />
        <head>
        
        <style>
        table {
          font-family: arial, sans-serif;
          border-collapse: collapse;
          width: 100%;
        }

        td, th {
          border: 1px solid #dddddd;
          text-align: left;
          padding: 8px;
        }

        tr:nth-child(even) {
          background-color: #dddddd;
        }
        </style>
        </head>
        <body>

        <h2>HTML Table</h2>
        $content 
        
        </body>
        </html>


    """.trimIndent()
    }


}