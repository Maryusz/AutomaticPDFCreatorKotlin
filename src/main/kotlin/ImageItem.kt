package com.mariuszbilda

import com.jfoenix.controls.JFXDialog
import com.jfoenix.controls.JFXDialogLayout
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.layout.AnchorPane
import java.awt.Desktop
import java.io.File
import java.net.URL
import java.util.*

class ImageItem : AnchorPane, Initializable {

    @FXML lateinit var iv_image: ImageView
    @FXML lateinit var lb_num_page: Label

    var mImageObjectProperty: ObjectProperty<Image>
    var imageFile: File
    var pageNumber: StringProperty = SimpleStringProperty()

    constructor(image: Image, file: File) {
        val loader = FXMLLoader(javaClass.getResource("/fxml/ImageItem.fxml"))
        loader.setController(this)
        loader.setRoot(this)
        loader.load<AnchorPane>()

        imageFile = file
        mImageObjectProperty = SimpleObjectProperty<Image>(image)
        iv_image.imageProperty().bind(mImageObjectProperty)
        lb_num_page.textProperty().bind(pageNumber)
    }

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        this.setOnMouseClicked {e ->

            // This causes to show the pressed image on the default program.
            if (e.button == MouseButton.PRIMARY) {
                try {
                    Desktop.getDesktop().open(imageFile)
                } catch (e: Exception) {
                    println(e.message)
                }
            }

        }
    }
}