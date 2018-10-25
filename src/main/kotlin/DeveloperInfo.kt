package com.mariuszbilda

import javafx.animation.RotateTransition
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.image.ImageView
import javafx.util.Duration
import java.net.URL
import java.util.*

class DeveloperInfo : Initializable {

    @FXML lateinit var programIcon: ImageView

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val rt = RotateTransition(Duration.seconds(2.0), programIcon)
        rt.fromAngle = 45.0
        rt.toAngle = 0.0
        rt.play()

    }
}