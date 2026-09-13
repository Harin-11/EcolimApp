package com.madrigalsolu.ecolima.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.madrigalsolu.ecolima.data.local.entity.RegistroEntity
import com.madrigalsolu.ecolima.data.local.entity.ResiduoTipo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting real PDF and Excel/CSV reports from local records.
 * Uses native Android PdfDocument and UTF-8 CSV compatible with Excel and Google Sheets.
 */
object ReportExporter {

    private fun getReportsDirectory(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Generates a real Excel-compatible CSV file with UTF-8 BOM.
     */
    fun exportCsv(context: Context, records: List<RegistroEntity>, period: String): File {
        val dir = getReportsDirectory(context)
        val sanitizedPeriod = period.lowercase().replace(" ", "_")
        val file = File(dir, "ECOLIM_Consolidado_${sanitizedPeriod}.csv")

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val sb = StringBuilder()
        // UTF-8 BOM so Excel opens it with proper accents/characters
        sb.append('\uFEFF')
        sb.append("Código,Fecha y Hora,Tipo de Residuo,Zona,Sector,Peso (kg),Volumen (m³),Usuario / Operario,Estado,EPP Completo,Observaciones\n")

        for (r in records) {
            val dateStr = sdf.format(Date(r.timestamp))
            val typeStr = when (r.tipoResiduo) {
                ResiduoTipo.PLASTICO -> "Plástico"
                ResiduoTipo.PAPEL_CARTON -> "Papel/Cartón"
                ResiduoTipo.ORGANICO -> "Orgánico"
                ResiduoTipo.PELIGROSO -> "Peligroso"
                ResiduoTipo.METAL -> "Metal"
                ResiduoTipo.VIDRIO -> "Vidrio"
            }
            val code = r.remoteId ?: "REC-${r.id}"
            val epp = if (r.eppCompleto) "Sí" else "No"
            val obs = (r.observaciones ?: "").replace("\"", "\"\"")
            val operario = r.operarioId.replace("\"", "\"\"")

            sb.append("\"$code\",")
            sb.append("\"$dateStr\",")
            sb.append("\"$typeStr\",")
            sb.append("\"${r.zona}\",")
            sb.append("\"${r.sector}\",")
            sb.append(String.format(Locale.US, "%.2f", r.pesoKg)).append(",")
            sb.append(String.format(Locale.US, "%.3f", r.volumenM3)).append(",")
            sb.append("\"$operario\",")
            sb.append("\"${r.estado.name}\",")
            sb.append("\"$epp\",")
            sb.append("\"$obs\"\n")
        }

        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    /**
     * Generates a real multi-page A4 PDF document.
     */
    fun exportPdf(
        context: Context,
        records: List<RegistroEntity>,
        period: String,
        totalWeight: Double,
        totalVolume: Double
    ): File {
        val dir = getReportsDirectory(context)
        val sanitizedPeriod = period.lowercase().replace(" ", "_")
        val file = File(dir, "ECOLIM_Reporte_${sanitizedPeriod}.pdf")

        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val rowsPerPage = 22

        val chunks = if (records.isEmpty()) listOf(emptyList()) else records.chunked(rowsPerPage)
        val totalPages = chunks.size
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val generatedAt = sdf.format(Date())

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subPaint = Paint().apply {
            color = Color.rgb(220, 240, 220)
            textSize = 10f
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val textPaint = Paint().apply {
            color = Color.rgb(55, 55, 55)
            textSize = 9.5f
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            color = Color.rgb(120, 120, 120)
            textSize = 8.5f
            isAntiAlias = true
        }

        for (pageIndex in chunks.indices) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            // Header banner
            val bannerPaint = Paint().apply { color = Color.rgb(46, 125, 50) } // Green #2E7D32
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 64f, bannerPaint)

            canvas.drawText("ECOLIM S.A.C. — Gestión y Trazabilidad de Residuos", 24f, 28f, titlePaint)
            canvas.drawText("Reporte Consolidado • Período: $period • Generado: $generatedAt", 24f, 48f, subPaint)

            var currentY = 85f

            // KPI Summary cards only on page 1
            if (pageIndex == 0) {
                val boxBg = Paint().apply { color = Color.rgb(240, 246, 240) }
                canvas.drawRoundRect(24f, currentY, pageWidth - 24f, currentY + 54f, 8f, 8f, boxBg)

                val kpiTitle = Paint().apply {
                    color = Color.rgb(46, 125, 50)
                    textSize = 12f
                    isFakeBoldText = true
                    isAntiAlias = true
                }
                val kpiSub = Paint().apply {
                    color = Color.rgb(70, 70, 70)
                    textSize = 9.5f
                    isAntiAlias = true
                }

                val colWidth = (pageWidth - 48f) / 3f
                // Col 1
                canvas.drawText("Total Registros", 36f, currentY + 20f, kpiSub)
                canvas.drawText("${records.size} recolecciones", 36f, currentY + 40f, kpiTitle)

                // Col 2
                canvas.drawText("Peso Total Recolectado", 36f + colWidth, currentY + 20f, kpiSub)
                canvas.drawText(String.format(Locale.US, "%,.1f kg", totalWeight), 36f + colWidth, currentY + 40f, kpiTitle)

                // Col 3
                canvas.drawText("Volumen Estimado", 36f + colWidth * 2, currentY + 20f, kpiSub)
                canvas.drawText(String.format(Locale.US, "%.2f m³", totalVolume), 36f + colWidth * 2, currentY + 40f, kpiTitle)

                currentY += 75f
            }

            // Table header
            val thBg = Paint().apply { color = Color.rgb(230, 235, 230) }
            canvas.drawRect(24f, currentY, pageWidth - 24f, currentY + 24f, thBg)

            canvas.drawText("Código", 30f, currentY + 16f, headerPaint)
            canvas.drawText("Fecha", 110f, currentY + 16f, headerPaint)
            canvas.drawText("Tipo Residuo", 210f, currentY + 16f, headerPaint)
            canvas.drawText("Zona / Sector", 320f, currentY + 16f, headerPaint)
            canvas.drawText("Peso (kg)", 460f, currentY + 16f, headerPaint)
            canvas.drawText("Estado", 520f, currentY + 16f, headerPaint)

            currentY += 24f

            val pageRecords = chunks[pageIndex]
            if (pageRecords.isEmpty()) {
                canvas.drawText("No se encontraron registros para este período.", 30f, currentY + 28f, textPaint)
            } else {
                var rowIndex = 0
                for (r in pageRecords) {
                    val rowBgColor = if (rowIndex % 2 == 0) Color.WHITE else Color.rgb(249, 250, 249)
                    val rowPaint = Paint().apply { color = rowBgColor }
                    canvas.drawRect(24f, currentY, pageWidth - 24f, currentY + 22f, rowPaint)

                    val code = (r.remoteId ?: "REC-${r.id}").take(12)
                    val date = sdf.format(Date(r.timestamp))
                    val type = when (r.tipoResiduo) {
                        ResiduoTipo.PLASTICO -> "Plástico"
                        ResiduoTipo.PAPEL_CARTON -> "Papel"
                        ResiduoTipo.ORGANICO -> "Orgánico"
                        ResiduoTipo.PELIGROSO -> "Peligroso"
                        ResiduoTipo.METAL -> "Metal"
                        ResiduoTipo.VIDRIO -> "Vidrio"
                    }
                    val zone = r.zona.take(18)
                    val weight = String.format(Locale.US, "%.1f", r.pesoKg)
                    val estado = if (r.estado.name == "SINCRONIZADO") "Sincronizado" else "Pendiente"

                    canvas.drawText(code, 30f, currentY + 15f, textPaint)
                    canvas.drawText(date, 110f, currentY + 15f, textPaint)
                    canvas.drawText(type, 210f, currentY + 15f, textPaint)
                    canvas.drawText(zone, 320f, currentY + 15f, textPaint)
                    canvas.drawText(weight, 460f, currentY + 15f, textPaint)
                    canvas.drawText(estado, 520f, currentY + 15f, textPaint)

                    currentY += 22f
                    rowIndex++
                }
            }

            // Footer
            val footerText = "Ecolim App • Página ${pageIndex + 1} de $totalPages • Documento de control ambiental"
            canvas.drawText(footerText, 24f, pageHeight - 20f, footerPaint)

            pdfDoc.finishPage(page)
        }

        FileOutputStream(file).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()
        return file
    }

    /**
     * Shares file via system Intent Chooser.
     */
    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Compartir $title").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }
}
