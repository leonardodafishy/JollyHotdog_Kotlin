package com.jolly_hotdogs.jollyhotdog

import javafx.fxml.FXML
import javafx.scene.control.Label

class HelloController {
    @FXML
    private val welcomeText: Label? = null
    @FXML
    protected fun onHelloButtonClick() {
        welcomeText!!.text = "Welcome to JavaFX Application!"
    }
}