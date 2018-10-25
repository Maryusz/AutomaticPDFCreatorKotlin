package com.mariuszbilda

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.stage.Stage
import javafx.util.Duration
import org.controlsfx.control.Notifications
import java.lang.Exception

import java.util.Scanner


class AutomaticPDFCreator : Application() {

    override fun start(primaryStage: Stage) {
        primaryStage.icons.add(Image("/icons/icons8_Parchment_96px.png"))
        val root = FXMLLoader.load<Parent>(javaClass.getResource("/fxml/MainScreen.fxml"))
        primaryStage.title = "Automatic PDF Creator - v. 0.6.7 Mariusz A. Bilda"
        primaryStage.scene = Scene(root)

        primaryStage.show()

        showNewFeatures()
    }

    private fun showNewFeatures() {
        val sc = Scanner(javaClass.getResourceAsStream("/changes/changes.txt"))
        var text = ""
        while (sc.hasNext()) {
            text += "\n" + sc.nextLine()
        }

        Notifications.create()
                .title("Aggiornamenti e note sulla nuova versione di Automatic PDF Creator!")
                .graphic(ImageView(Image(javaClass.getResourceAsStream("/icons/icons8_Parchment_96px.png"))))
                .text(text)
                .hideAfter(Duration.seconds(30.0))
                .show()


    }

}

fun main(args: Array<String>) {
    Application.launch(AutomaticPDFCreator().javaClass)
}