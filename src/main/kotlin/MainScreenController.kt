package com.mariuszbilda

import animatefx.animation.*
import com.jfoenix.controls.JFXCheckBox
import com.jfoenix.controls.JFXProgressBar
import com.jfoenix.controls.JFXSlider
import javafx.application.Platform
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.concurrent.Task
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import org.controlsfx.control.Notifications

import java.io.*
import java.net.URL
import java.nio.file.*
import java.util.LinkedHashMap
import java.util.Properties
import java.util.ResourceBundle
import java.util.logging.Level
import java.util.logging.Logger

class MainScreenController : Initializable {

    private var properties: Properties = Properties()
    private var logger: Logger = Logger.getLogger(javaClass.name)
    private var pathToObserve: String = ""
    private var saveDirectory: String = ""
    private var listOfFiles: MutableMap<File, ImageView> = mutableMapOf()
    private var directoryChooser: DirectoryChooser = DirectoryChooser()
    private var pageCounter: IntegerProperty = SimpleIntegerProperty()

    @FXML private lateinit var root: AnchorPane
    @FXML private lateinit var imageBox: HBox
    @FXML private lateinit var labelNumberOfPages: Label
    @FXML private lateinit var labelWatchedDirectory: Label
    @FXML private lateinit var labelSaveDir: Label
    @FXML private lateinit var checkboxDelete: JFXCheckBox
    @FXML private lateinit var progressBar: JFXProgressBar
    @FXML private lateinit var menuItemReset: MenuItem
    @FXML private lateinit var compressionFactorSlider: JFXSlider

    override fun initialize(location: URL?, resources: ResourceBundle?) {

        //Essential initializations
        imageBox.isCache = true
        logger = Logger.getLogger(javaClass.getName())
        listOfFiles = LinkedHashMap()
        properties = Properties()

        // Counter of pages and its binding
        pageCounter = SimpleIntegerProperty(0)
        labelNumberOfPages.textProperty().bind(pageCounter.asString())

        labelNumberOfPages.textProperty().addListener({_, _, n ->
            RotateIn(labelNumberOfPages).play()
        })

        // Setting loading, and absence of a setting file managed
        try {
            logger.log(Level.INFO, "Trying to load properties...")
            properties.loadFromXML(FileInputStream("settings.xml"))

            pathToObserve = properties.getProperty("directoryToWatch")
            saveDirectory = properties.getProperty("saveDirectory")
        } catch (ioe: IOException) {
            logger.log(Level.SEVERE, ioe.toString())
            try {
                logger.log(Level.WARNING, "Creating a new empty setting file.")
                val newSettingsFile = FileOutputStream("settings.xml")
                properties.setProperty("saveDirectory", "C:\\")
                properties.setProperty("directoryToWatch", "C:\\")
                properties.storeToXML(newSettingsFile, "")

            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        // Setting of folders and start of watch service
        labelWatchedDirectory.text = pathToObserve
        labelSaveDir.text = saveDirectory

        checkboxDelete.selectedProperty().addListener { observable ->
            if (checkboxDelete.isSelected) {
                val content = "Abilitando questa impostazione i file immagine originali che verranno utilizzati per creare il PDF verranno eliminati NESSUNA senza possibilità di recupero!"
                val a = Alert(Alert.AlertType.WARNING, content)
                a.showAndWait()
            }
        }

        menuItemReset.setOnAction { e ->
            pageCounter.setValue(0)
            for (f in listOfFiles.keys) {
                f.delete()
                logger.log(Level.WARNING, String.format("%s deleted.", f))
            }

            listOfFiles.clear()

            Platform.runLater { imageBox.children.clear() }

        }
        watcherService()
    }


    @FXML fun chooseDirectory(event: ActionEvent) {

        directoryChooser = DirectoryChooser()
        val file = directoryChooser.showDialog(root.scene.window)
        properties.setProperty("directoryToWatch", file.path)
        pathToObserve = file.path
        labelWatchedDirectory.text = pathToObserve

        watcherService()
    }

    fun chooseDestinationDir(actionEvent: ActionEvent) {

        directoryChooser = DirectoryChooser()
        val file = directoryChooser.showDialog(root.scene.window)
        properties.setProperty("saveDirectory", file.path)
        saveDirectory = file.path
        labelSaveDir.text = saveDirectory
        try {
            properties.storeToXML(FileOutputStream("settings.xml"), "")

            logger.log(Level.INFO, "Properties saved")
        } catch (ioe: IOException) {
            logger.log(Level.SEVERE, ioe.toString())
        }

    }

    /**
     * This method launches a Task who observe the directory choosen and when an image is saved to that directoy
     * it adds it to the visual part (HBox) and to the data structure (in this case a LinkedHashMap, for order preservation)
     */
    private fun watcherService() {

        try {
            val ws = FileSystems.getDefault().newWatchService()
            val path = Paths.get(pathToObserve)

            val watchKey = path.register(ws, StandardWatchEventKinds.ENTRY_CREATE)

            val task = object : Task<Void>() {
                override fun call(): Void? {

                    var key: WatchKey?
                    try {
                        while (true) {
                            key = ws.take()
                            for (event in key.pollEvents()) {

                                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {

                                    Platform.runLater {


                                        val file = File(pathToObserve + "\\" + event.context())

                                        /**
                                         * The WatcherService detects if the new file in the directory is a File or a Directory,
                                         * if its a file, it porcess it as normale, if its a directory, an alert is showed
                                         * and its asked if the user want to change the watched directory to the directory registered.
                                         */
                                        if (file.isFile) {

                                            // sleeper task will give time to the other process to save the image so only the APDFC can load it.
                                            val sleeper = object : Task<Void>() {
                                                override fun call(): Void? {
                                                    try {
                                                        Thread.sleep(1000)
                                                    } catch (e: InterruptedException) {
                                                        logger.log(Level.SEVERE, "Applicazione interrotta\n" + e.message)
                                                    }

                                                    return null
                                                }
                                            }

                                            Thread(sleeper).start()

                                            sleeper.setOnSucceeded { e -> fileDetected(event, file) }

                                        }
                                        if (file.isDirectory) {
                                            directoryDetected(event)
                                        }
                                    }
                                }
                            }
                            key.reset()
                        }
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }

                    return null
                }
            }

            val t = Thread(task).apply { isDaemon = true }
            t.start()


        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun fileDetected(event: WatchEvent<*>, file: File) {

        pageCounter.value = pageCounter.value!! + 1
        val ii = ImageItem(Image("file:" + pathToObserve + "\\" + event.context(), 200.0, 300.0, true, false), File(pathToObserve + "\\" + event.context()))
        addContextMenu(ii)

        listOfFiles[file] = ii.iv_image
        ii.pageNumber.bind(pageCounter.asString())
        ii.pageNumber.unbind()

        // TODO: Aspettare l'aggiornamento di CONTROLSFX per rilasciare la versione che utilizza questo metodo.
        // FIX: showPageNotification(Image("file:" + pathToObserve + "\\" + event.context(), 200.0, 300.0, true, false))

        imageBox.children.add(ii)
        SlideInDown(ii).play()
        logger.log(Level.INFO, "Adding image to HBox")

    }

    @Deprecated("Wait to an CONTROLSFX update for use of this method.")
    private fun showPageNotification(image: Image) {
        //This shows an windows notification when a image is added!
        Notifications.create()
                .title("Pagina aggiunta")
                .text("Una nuova pagina è stata aggiunta al documento")
                .graphic(ImageView(image))
                .show()

    }

    private fun directoryDetected(event: WatchEvent<*>) {
        logger.log(Level.INFO, "Directory creation detected...")
        val content = "E' stata rilveta la creazione di una sottocartella nella cartella da te osservata, " +
                "seleziona il tasto OK se non sei certo e prova a fare una scansione, altrimenti se la cartella l'hai creata tu per altri scopi," +
                "seleziona il tasto Annulla"
        val a = Alert(Alert.AlertType.CONFIRMATION, content)
        if (a.showAndWait().get() == ButtonType.OK) {
            pathToObserve += "\\" + event.context()


            logger.log(Level.INFO, "New path to observe: " + pathToObserve)

            // Restart WatchService
            watcherService()
        }
    }

    /**
     * This method instantiate com.mariuszbilda.PDFManager, and permits to add pages taking path from listOfFiles,
     * then clear the GUI, create the PDF file and delete or not the files used.
     *
     * A task is launched to not freeze the gui, and the progress bar is updated for the creation of single pages.
     * @param actionEvent
     */
    fun createPDFFile(actionEvent: ActionEvent) {
        val pdfCreationTask = object : Task<Void>() {
            override fun call(): Void? {
                updateProgress(0.0, 1000.0)

                if (listOfFiles.keys.size == 0) {
                    // If there's no images to transform in PDF, an alert its showed
                    Platform.runLater { showNoImageAlert() }


                } else {
                    progressBar.id ="bar"
                    Platform.runLater {
                        pageCounter.set(0)

                    }

                    val pdfManager = PDFManager()
                    pdfManager.compressionFactor.set(compressionFactorSlider.valueProperty().floatValue() / 100.0f)

                    // calculation for progress bar
                    val part = 1000.0 / listOfFiles.keys.size
                    var actual = 0.0
                    for (f in listOfFiles.keys) {
                        actual += part
                        pdfManager.addPage(f)

                        updateProgress(actual, 1000.0)
                        logger.log(Level.INFO, "Page added.")
                    }

                    pdfManager.savePDF(saveDirectory)
                    updateProgress(1000.0, 1000.0)

                    if (checkboxDelete.isSelected) {
                        for (f in listOfFiles.keys) {
                            f.delete()
                            logger.log(Level.WARNING, String.format("%s deleted.", f))
                        }
                    }
                    Platform.runLater {

                        imageBox.children.clear()
                        listOfFiles.clear()
                        Notifications.create()
                                .title("PDF correttamente creato!")
                                .text("PDF correttamente creato!")
                                .graphic(ImageView(Image(javaClass.getResourceAsStream("/icons/icons8_Checkmark_96px_1.png"))))
                                .show()
                    }
                }

                updateProgress(0.0, 1000.0)

                return null
            }
        }

        progressBar.progressProperty().bind(pdfCreationTask.progressProperty())

        val t = Thread(pdfCreationTask)
        t.isDaemon = true
        t.start()

    }

    fun showLogWindow(actionEvent: ActionEvent) {
        //TODO: show log on another window...
    }

    private fun addContextMenu(it: ImageItem) {
        it.setOnContextMenuRequested { event ->
            val deleteImage = MenuItem("Cancella")

            val cm = ContextMenu(deleteImage)

            deleteImage.setOnAction { event1 ->
                imageBox.children.remove(it)
                var keyToRemove = File("")
                for ((key, value) in listOfFiles) {
                    if (value == it.iv_image) {
                        keyToRemove = key
                    }
                }

                listOfFiles.remove(keyToRemove)
                keyToRemove.delete()
                pageCounter.set(pageCounter.get() - 1)

            }

            cm.show(it, event.screenX, event.screenY)

        }


    }

    fun showDeveloperInfo(actionEvent: ActionEvent) {

        try {
            val stage = Stage()
            stage.icons.add(Image("/icons/icons8_Parchment_96px.png"))
            val parent = FXMLLoader.load<Parent>(javaClass.getResource("/fxml/DeveloperInfo.fxml"))
            val scene = Scene(parent)
            stage.scene = scene
            stage.showAndWait()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun showNoImageAlert() {
        val alert = Alert(Alert.AlertType.WARNING)
        alert.headerText = "Nessuna immagine rilevata!"
        alert.title = "Nessuna immagine da trasformare in PDF."
        alert.contentText = "Non è stata rilevata nessuna immagine da trasformare in PDF, " +
                "verifica che ci sia almeno un'immagine (presente nella schermata o rilevata tramite numero)" +
                " altrimenti non sarà possibile creare il file PDF!"
        alert.showAndWait()
    }
}
