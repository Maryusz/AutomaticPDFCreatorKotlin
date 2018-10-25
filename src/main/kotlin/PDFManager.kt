package com.mariuszbilda

import javafx.beans.property.FloatProperty
import javafx.beans.property.SimpleFloatProperty
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject

import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import javax.imageio.stream.ImageOutputStream
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.logging.Level
import java.util.logging.Logger

class PDFManager {
    var compressionFactor: FloatProperty
    private val doc: PDDocument
    private val logger: Logger
    private var pageNumber: Int = 0
    private val tmpDir: File

    init {
        doc = PDDocument()
        logger = Logger.getLogger("PDF MANAGER")
        pageNumber = 0
        compressionFactor = SimpleFloatProperty()
        tmpDir = File(System.getProperty("java.io.tmpdir"))
    }

    /**
     * https://gist.github.com/mmdemirbas/209b4bdb66b788e785266f97204b8631
     * This method take a path of one image and add it to the PDF document.
     * @param file image to add
     */

    fun addPage(file: File) {
        var file = file


        pageNumber++
        try {
            file = compressImage(file)

            val image = ImageIO.read(file)

            if (image.width > image.height) {file = rotate(file)}

            val pdImageXObject = PDImageXObject.createFromFileByContent(file, doc)
            val pageSize = PDRectangle.A4

            val originalWidth = pdImageXObject.width
            val originalHeight = pdImageXObject.height

            logger.log(Level.INFO, String.format("Original sizes: w: %d h: %d", originalWidth, originalHeight))


            val pageWidth = pageSize.width
            val pageHeight = pageSize.height

            logger.log(Level.INFO, String.format("Page sizes: w: %.2f h: %.2f", pageWidth, pageHeight))

            val ratio = Math.min(pageWidth / originalWidth, pageHeight / originalHeight)

            logger.log(Level.INFO, String.format("RATIO: %.2f", ratio))

            val scaledWidth = originalWidth * ratio // (pageWidth / originalWidth)
            val scaledHeight = originalHeight * ratio //(pageHeight / originalHeight)

            logger.log(Level.INFO, String.format("Scaled: w: %.2f h: %.2f", scaledWidth, scaledHeight))

            val x = (pageWidth - scaledWidth) / 2
            val y = (pageHeight - scaledHeight) / 2

            logger.log(Level.INFO, String.format("X: %.2f Y: %.2f", x, y))

            val page = PDPage(pageSize)

            doc.addPage(page)


            PDPageContentStream(doc, page).use { contentStream ->

                contentStream.drawImage(pdImageXObject, x, y, scaledWidth, scaledHeight)

                /**
                 * This part "signs" the pdf and adds the pagination
                 */
                contentStream.beginText()
                contentStream.newLineAtOffset(scaledWidth / 2, 2f)
                contentStream.setFont(PDType1Font.HELVETICA, 6f)
                contentStream.showText("APDFC 0.6.7 - Page:   $pageNumber")
                contentStream.endText()

            }


        } catch (e: IOException) {
            e.printStackTrace()
        }

    }


    fun savePDF(destinationPath: String?) {
        try {
            logger.log(Level.INFO, "Saving document into path: $destinationPath")
            doc.save(destinationPath + String.format("/%s.pdf", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HHmmss"))))
            doc.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }


    /**
     * This method compress the image received as File
     * The compressed image is placed in TMP folder!
     *
     * @param file File of the image received
     * @return path to the compressed image
     */
    private fun compressImage(file: File): File {
        logger.log(Level.INFO, "Starting image compression...")
        val compressedImage = File(tmpDir.path + "//compressed-tmp.jpg")
        println(compressedImage)

        try {
            val image = ImageIO.read(file)

            val os = FileOutputStream(compressedImage)

            val writer = ImageIO.getImageWritersByFormatName("jpg")
            val imageWriter = writer.next()

            val ios = ImageIO.createImageOutputStream(os)
            imageWriter.output = ios

            val param = imageWriter.defaultWriteParam
            param.compressionMode = ImageWriteParam.MODE_EXPLICIT
            logger.log(Level.INFO, String.format("Compression factor: %.2f", getCompressionFactor()))
            param.compressionQuality = getCompressionFactor()
            imageWriter.write(null, IIOImage(image, null, null), param)

            os.close()
            ios.close()
            imageWriter.dispose()

        } catch (e: IOException) {
            logger.log(Level.INFO, "Error while compressing the image.")
            e.printStackTrace()

        }

        logger.log(Level.INFO, "Image compressed succesfully!")
        return compressedImage
    }

    /**
     * This method receives an File containing an image and rotates it by 90 degress
     * The new image is a temp file and its returned as JPG
     */
    private fun rotate(file: File): File {
        logger.log(Level.INFO, "Image rotation launched...")
        val image = ImageIO.read(file)
        val width = image.width
        val height = image.height

        val tx = AffineTransform()
        tx.translate(image.height / 2.0, image.width / 2.0)
        tx.rotate(Math.PI / 2)
        tx.translate( -image.width.div(2.0), -image.height.div(2.0))


        val op = AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR)

        val newImage = BufferedImage(height, width, image.type)
        op.filter(image, newImage)

        val f = File.createTempFile("tmp_rotated_image", ".jpg")
        ImageIO.write(newImage, "jpg", f)
        logger.log(Level.INFO, "Image rotated succesfully!")
        return f
    }

    fun getCompressionFactor(): Float {
        return compressionFactor.get()
    }

    fun setCompressionFactor(compressionFactor: Float) {
        this.compressionFactor.set(compressionFactor)
    }

    fun compressionFactorProperty(): FloatProperty {
        return compressionFactor
    }
}