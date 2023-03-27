package com.jolly_hotdogs.jollyhotdog

import javafx.application.Application
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import javafx.util.Callback
import java.io.*
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Consumer


class Main : Application() {
    // UI components
    private var inventoryTable: TableView<Item?>? = null
    private var salesTable: TableView<Sales>? = null
    private var totalTable: TableView<TotalSales>? = null
    private val newTable: TableView<Item>? = null
    private var filterTypeComboBox: ComboBox<String?>? = null
    private var typeComboBox: ComboBox<String>? = null
    private var itemSalesComboBox: ComboBox<String>? = null
    private var nameField: TextField? = null
    private var quantityField: TextField? = null
    private var priceField: TextField? = null
    private var quantitySalesField: TextField? = null
    private var branchField: TextField? = null
    private var priceSalesField: TextField? = null
    private var typeSalesField: TextField? = null
    private var statusLabel: Label? = null
    private var searchLabel: Label? = null
    private var salesLabel: Label? = null

    // inventory data
    private val inventoryData = FXCollections.observableArrayList<Item?>()
    private val inventoryList: MutableList<Item?> = ArrayList()
    private val salesData = FXCollections.observableArrayList<Sales>()
    private val salesList: MutableList<Sales> = ArrayList()
    private val totalSalesData = FXCollections.observableArrayList(
        TotalSales(0.0, 0.0, 0.0)
    )
    private var searchText = ""

    // file handling
    private var inventoryFile: File? = null
    private var salesFile: File? = null
    override fun start(stage: Stage) {
        // create UI components
        val titleLabel = Label("Jolly Hotdogs POS")
        titleLabel.style = "-fx-font-size: 24px; -fx-font-weight: bold;"
        titleLabel.alignment = Pos.CENTER
        val filterBox = HBox()
        filterBox.spacing = 10.0
        filterBox.alignment = Pos.CENTER_LEFT
        filterTypeComboBox = ComboBox()
        filterTypeComboBox!!.value = "All"
        filterTypeComboBox!!.promptText = "Filter by Type"
        filterTypeComboBox!!.items.addAll("All", "Drink", "Sides", "Mini Dog", "Hotdog", "Hotdog Sandwich")
        filterTypeComboBox!!.onAction = EventHandler { event: ActionEvent? -> filterInventory() }
        val searchField = TextField()
        searchField.promptText = "Search by Name"
        searchField.onKeyReleased = EventHandler { event: KeyEvent? -> setSearchGlobal(searchField) }
        val removeButton = Button("Remove Selected")
        removeButton.onAction = EventHandler { event: ActionEvent? -> removeItem() }
        removeButton.alignment = Pos.CENTER_RIGHT
        val increaseButton = Button("Add Selected")
        increaseButton.onAction = EventHandler { event: ActionEvent? -> increaseItem() }
        increaseButton.alignment = Pos.CENTER_RIGHT
        val priceButton = Button("Set Price Selected")
        priceButton.onAction = EventHandler { event: ActionEvent? -> changePrice() }
        priceButton.alignment = Pos.CENTER_RIGHT
        val managerButton = Button("Transaction Manager")
        managerButton.onAction = EventHandler { event: ActionEvent? -> transactionManager() }
        managerButton.alignment = Pos.CENTER_RIGHT
        searchLabel = Label()
        searchLabel!!.style = "-fx-font-size: 12px;"
        searchLabel!!.alignment = Pos.CENTER_LEFT
        filterBox.children.addAll(
            filterTypeComboBox,
            searchField,
            removeButton,
            increaseButton,
            priceButton,
            managerButton
        )
        inventoryTable = TableView()
        inventoryTable!!.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
        inventoryTable!!.columns.forEach(Consumer { column: TableColumn<Item?, *> ->
            column.isResizable = false
            column.setMinWidth(50.0)
        })
        inventoryTable!!.isEditable = false
        val nameColumn = TableColumn<Item?, String>("Name")
        nameColumn.setCellValueFactory { cellData: TableColumn.CellDataFeatures<Item?, String> ->
            val item = cellData.value
            item?.name
        }

        val quantityColumn = TableColumn<Item?, String>("Quantity")
        quantityColumn.setCellValueFactory { cellData: TableColumn.CellDataFeatures<Item?, String> ->
            SimpleStringProperty(
                cellData.value!!.quantity.toString()
            )
        }
        quantityColumn.setCellFactory { column: TableColumn<Item?, String>? ->
            object : TableCell<Item?, String?>() {
                override fun updateItem(item: String?, empty: Boolean) {
                    super.updateItem(item, empty)
                    if (item == null || empty) {
                        text = null
                        style = ""
                    } else {
                        text = item
                        val quantity = item.toInt()
                        style = if (quantity == 0) {
                            "-fx-background-color: red;"
                        } else {
                            ""
                        }
                    }
                }
            }
        }
        val typeColumn = TableColumn<Item?, String>("Type")
        typeColumn.setCellValueFactory { cellData: TableColumn.CellDataFeatures<Item?, String> ->
            SimpleStringProperty(
                cellData.value!!.type.displayName
            )
        }
        val priceColumn = TableColumn<Item?, String>("Price")
        priceColumn.setCellValueFactory { cellData: TableColumn.CellDataFeatures<Item?, String> ->
            SimpleStringProperty(
                cellData.value!!.price.toString()
            )
        }
        val lastTransactionColumn = TableColumn<Item?, String>("Last Transaction")
        lastTransactionColumn.setCellValueFactory { cellData: TableColumn.CellDataFeatures<Item?, String> ->
            SimpleStringProperty(
                cellData.value!!.lastTransaction.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            )
        }
        nameColumn.prefWidthProperty().bind(inventoryTable!!.widthProperty().multiply(0.3)) // 60% of the table width
        quantityColumn.prefWidthProperty()
            .bind(inventoryTable!!.widthProperty().multiply(0.1)) // 40% of the table width
        typeColumn.prefWidthProperty().bind(inventoryTable!!.widthProperty().multiply(0.2)) // 60% of the table width
        priceColumn.prefWidthProperty().bind(inventoryTable!!.widthProperty().multiply(0.1)) // 40% of the table width
        lastTransactionColumn.prefWidthProperty()
            .bind(inventoryTable!!.widthProperty().multiply(0.3)) // 40% of the table width
        inventoryTable!!.columns.addAll(
            listOf(
                nameColumn,
                quantityColumn,
                typeColumn,
                priceColumn,
                lastTransactionColumn
            )
        )
        inventoryTable!!.items = inventoryData
        val inputBox = HBox()
        inputBox.spacing = 10.0
        inputBox.alignment = Pos.CENTER
        nameField = TextField()
        nameField!!.promptText = "Name"
        quantityField = TextField()
        quantityField!!.promptText = "Quantity"
        typeComboBox = ComboBox()
        typeComboBox!!.promptText = "Type"
        typeComboBox!!.items.addAll("Drink", "Sides", "Mini Dog", "Hotdog", "Hotdog Sandwich")
        priceField = TextField()
        priceField!!.promptText = "Price"
        val addButton = Button("Add New Item")
        addButton.onAction = EventHandler { event: ActionEvent? -> addItem() }
        inputBox.children.addAll(nameField, quantityField, typeComboBox, priceField, addButton)
        val importButton = Button("Import")
        importButton.onAction = EventHandler { event: ActionEvent? -> importInventory(stage) }
        val exportButton = Button("Export")
        exportButton.onAction = EventHandler { event: ActionEvent? -> exportInventory(stage) }
        val fileBox = HBox()
        fileBox.spacing = 10.0
        fileBox.alignment = Pos.CENTER_LEFT
        fileBox.children.addAll(importButton, exportButton)
        statusLabel = Label()
        statusLabel!!.style = "-fx-font-size: 12px;"
        statusLabel!!.alignment = Pos.CENTER
        val root = VBox()
        val stream: InputStream = FileInputStream("D:\\Users\\Ewan\\IdeaProjects\\JollyHotdog_POS-master\\src\\main\\resources\\com\\jolly_hotdogs\\jollyhotdog\\hotdog.png")
        val image = Image(stream)
        val imageView = ImageView()
        imageView.image = image
        imageView.x = 10.0
        imageView.y = 10.0
        imageView.fitWidth = 100.0
        imageView.isPreserveRatio = true
        root.spacing = 10.0
        root.padding = Insets(10.0)
        root.alignment = Pos.TOP_CENTER
        root.children.add(imageView)
        root.children.addAll(titleLabel, filterBox, searchLabel, inventoryTable, inputBox, fileBox, statusLabel)
        val scene = Scene(root, 800.0, 600.0)
        inventoryTable!!.prefHeightProperty().bind(scene.heightProperty())
        stage.icons.add(Image("D:\\Users\\Ewan\\IdeaProjects\\JollyHotdog_POS-master\\src\\main\\resources\\com\\jolly_hotdogs\\jollyhotdog\\hotdog.png"))
        stage.scene = scene
        stage.title = "Inventory Manager"
        stage.show()
    }

    private fun filterInventory() {
        filterInventoryByType()
    }

    // filters the inventory table based on the type
    private fun filterInventoryByType() {
        val filterType = if (filterTypeComboBox!!.value == null) "All" else filterTypeComboBox!!.value!!
        val filteredData = FXCollections.observableArrayList<Item?>()
        if (filterType == "All" && searchText.isEmpty()) {
            filteredData.addAll(inventoryData)
        } else {
            filteredData.addAll(inventoryData.stream()
                .filter { item: Item? ->
                    (filterType == "All" || item!!.type.toString() == filterType.replace(" ", "_")
                        .uppercase(Locale.getDefault())) &&
                            (searchText.isEmpty() || item!!.name.value.lowercase(Locale.getDefault())
                                .contains(searchText.lowercase(Locale.getDefault())))
                }
                .toList())
            if (searchText.isNotEmpty()) {
                searchLabel!!.text = "Results for: $searchText"
            } else {
                searchLabel!!.text = ""
            }
        }
        inventoryTable!!.items = filteredData
    }

    private fun changePrice() {
        val selectedItem = inventoryTable!!.selectionModel.selectedItem
        if (selectedItem != null) {
            val dialog = Dialog<String?>()
            dialog.title = "Change Price"
            dialog.headerText = "Enter new price for " + selectedItem.name.value
            val confirmButton = ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE)
            dialog.dialogPane.buttonTypes.addAll(confirmButton, ButtonType.CANCEL)
            val priceField = TextField(selectedItem.price.toString())
            priceField.promptText = "Price"
            val vbox = VBox()
            vbox.children.addAll(Label("New Price:"), priceField)
            dialog.dialogPane.content = vbox
            dialog.setResultConverter { dialogButton: ButtonType ->
                if (dialogButton == confirmButton) {
                    return@setResultConverter priceField.text
                }
                null
            }
            val result = dialog.showAndWait()
            result.ifPresent { price: String? ->
                try {
                    val newPrice = price!!.toDouble()
                    selectedItem.price = newPrice
                    inventoryTable!!.refresh()
                    statusLabel!!.text = "Price for " + selectedItem.name.value + " changed to " + newPrice
                    selectedItem.lastTransaction = LocalDateTime.now()
                } catch (e: NumberFormatException) {
                    statusLabel!!.text = "Error: Invalid input for price"
                }
            }
        } else {
            statusLabel!!.text = "Error: No item selected"
        }
    }

    private fun setSearchGlobal(searchField: TextField) {
        searchText = searchField.text
        filterInventory()
    }

    // adds a new item to the inventory
    private fun addItem() {
        val name = nameField!!.text.trim { it <= ' ' }
        if (name.isEmpty()) {
            statusLabel!!.text = "Error: Name cannot be empty"
            return
        }
        val type = ItemType.valueOf(typeComboBox!!.value.replace(" ", "_").uppercase(Locale.getDefault()))
        if (type == null) {
            statusLabel!!.text = "Error: Type cannot be empty"
            return
        }
        val quantity: Int
        try {
            quantity = quantityField!!.text.trim { it <= ' ' }.toInt()
            if (quantity < 0) {
                statusLabel!!.text = "Error: Quantity cannot be negative"
                return
            }
        } catch (e: NumberFormatException) {
            statusLabel!!.text = "Error: Invalid quantity"
            return
        }
        val price: Double
        try {
            price = priceField!!.text.trim { it <= ' ' }.toDouble()
            if (price < 0) {
                statusLabel!!.text = "Error: Price cannot be negative"
                return
            }
        } catch (e: NumberFormatException) {
            statusLabel!!.text = "Error: Invalid price"
            return
        }
        val newItem = Item(name, quantity, type, price, LocalDateTime.now())
        inventoryList.add(newItem)
        inventoryData.add(newItem)
        statusLabel!!.text = "Added $name"
        filterInventory()
    }

    // removes an item from the inventory
    private fun removeItem() {
        val selectedItem = inventoryTable!!.selectionModel.selectedItem
        if (selectedItem != null) {
            val selectedQuantity = selectedItem.quantity
            val dialog = TextInputDialog(selectedQuantity.toString())
            dialog.title = "Remove Item"
            dialog.headerText = "Remove " + selectedItem.name.value + " - Quantity: " + selectedQuantity
            dialog.contentText = "Enter quantity to remove:"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                val input = result.get()
                try {
                    val quantityToRemove = input.toInt()
                    if (quantityToRemove in 1..selectedQuantity) {
                        selectedItem.quantity = selectedQuantity - quantityToRemove
                        selectedItem.lastTransaction = LocalDateTime.now()
                        inventoryTable!!.refresh()
                        statusLabel!!.text =
                            "Removed " + quantityToRemove + " " + selectedItem.name.value + " - Quantity: " + selectedQuantity
                    } else {
                        val alert = Alert(Alert.AlertType.WARNING)
                        alert.title = "Invalid Quantity"
                        alert.headerText = null
                        alert.contentText = "Please enter a valid quantity to remove."
                        alert.showAndWait()
                    }
                } catch (e: NumberFormatException) {
                    val alert = Alert(Alert.AlertType.WARNING)
                    alert.title = "Invalid Input"
                    alert.headerText = null
                    alert.contentText = "Please enter a valid number."
                    alert.showAndWait()
                }
            }
        }
    }

    private fun increaseItem() {
        val selectedItem = inventoryTable!!.selectionModel.selectedItem
        if (selectedItem != null) {
            val dialog = TextInputDialog()
            dialog.title = "Increase Quantity"
            dialog.headerText = "Enter quantity to add:"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                val quantityToAdd = result.get().toInt()
                val newQuantity = selectedItem.quantity + quantityToAdd
                selectedItem.lastTransaction = LocalDateTime.now()
                selectedItem.quantity = newQuantity
                inventoryTable!!.refresh()
                statusLabel!!.text = "Increased quantity of " + selectedItem.name.value + " by " + quantityToAdd
            }
        }
    }

    // imports items to the inventory from a CSV file
    private fun importInventory(stage: Stage) {
        val fileChooser = FileChooser()
        fileChooser.title = "Select Inventory File"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("CSV Files", "*.csv"))
        val selectedFile = fileChooser.showOpenDialog(stage)
        inventoryFile = selectedFile
        if (selectedFile != null) {
            try {
                Scanner(selectedFile, StandardCharsets.UTF_8).use { scanner ->
                    val importedItems: MutableList<Item?> = ArrayList()
                    scanner.nextLine() // skip header
                    while (scanner.hasNextLine()) {
                        val line = scanner.nextLine()
                        val parts = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val name = parts[0].trim { it <= ' ' }
                        val quantity = parts[1].trim { it <= ' ' }.toInt()
                        val type = ItemType.valueOf(parts[2].trim { it <= ' ' })
                        val price = parts[3].trim { it <= ' ' }.toDouble()
                        val lastTransaction = LocalDateTime.parse(parts[4].trim { it <= ' ' })
                        val newItem = Item(name, quantity, type, price, lastTransaction)
                        importedItems.add(newItem)
                    }
                    inventoryList.addAll(importedItems)
                    inventoryData.addAll(importedItems)
                    statusLabel!!.text = "Imported " + importedItems.size + " items from " + selectedFile.name
                }
            } catch (e: IOException) {
                statusLabel!!.text = "Error importing items from " + selectedFile.name
            }
        }
        filterInventory()
    }

    // exports the inventory to a CSV file
    private fun exportInventory(stage: Stage) {
        val fileChooser = FileChooser()
        fileChooser.title = "Save Inventory File"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("CSV Files", "*.csv"))
        val selectedFile = fileChooser.showSaveDialog(stage)
        if (selectedFile != null) {
            try {
                PrintWriter(FileOutputStream(selectedFile), true, StandardCharsets.UTF_8).use { writer ->
                    writer.println("Name,Quantity,Type,Price,Last Transaction")
                    for (item in inventoryList) {
                        val name = item!!.name.value
                        val type = item.type
                        val lastTransaction = item.lastTransaction
                        writer.println(name + "," + item.quantity + "," + type + "," + item.price + "," + lastTransaction)
                    }
                    statusLabel!!.text = "Inventory exported to " + selectedFile.name
                }
            } catch (e: IOException) {
                statusLabel!!.text = "Error exporting inventory to " + selectedFile.name
            }
        }
    }

    private fun transactionManager() {
        val stage = Stage()
        stage.icons.add(Image("D:\\Users\\Ewan\\IdeaProjects\\JollyHotdog_POS-master\\src\\main\\resources\\com\\jolly_hotdogs\\jollyhotdog\\hotdog.png"))
        stage.title = "Transaction Manager"
        val titleLabel = Label("Sales Report")
        titleLabel.style = "-fx-font-size: 24px; -fx-font-weight: bold;"
        titleLabel.alignment = Pos.CENTER
        val branchLabel = Label("Branch: ")
        branchLabel.style = "-fx-font-size: 20px;"
        branchField = TextField("FEU TECH")
        branchField!!.style = "-fx-font-size: 20px; -fx-font-weight: bold;"
        val dateLabel = Label("Date: " + LocalDate.now().toString())
        dateLabel.style = "-fx-font-size: 18px"
        val branchAndDateBox = VBox(10.0, branchLabel, branchField, dateLabel)
        salesTable = TableView()
        salesTable!!.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
        salesTable!!.columns.forEach(Consumer { column: TableColumn<Sales, *> ->
            column.isResizable = false
            column.setMinWidth(50.0)
        })
        salesTable!!.isEditable = false
        val nameColumn = TableColumn<Sales, String>("Name")
        nameColumn.cellValueFactory =
            Callback { cellData: TableColumn.CellDataFeatures<Sales, String> -> SimpleStringProperty(cellData.value.name) }
        val quantityColumn = TableColumn<Sales, String>("Quantity")
        quantityColumn.cellValueFactory = Callback { cellData: TableColumn.CellDataFeatures<Sales, String> ->
            SimpleStringProperty(
                cellData.value.quantity.toString()
            )
        }
        val typeColumn = TableColumn<Sales, String>("Type")
        typeColumn.cellValueFactory = Callback { cellData: TableColumn.CellDataFeatures<Sales, String> ->
            val sales = cellData.value
            SimpleStringProperty(cellData.value.type.displayName)
        }
        val priceColumn = TableColumn<Sales, String>("Price")
        priceColumn.cellValueFactory = Callback { cellData: TableColumn.CellDataFeatures<Sales, String> ->
            SimpleStringProperty(
                cellData.value.price.toString()
            )
        }
        val grossColumn = TableColumn<Sales, String>("Gross Income")
        grossColumn.cellValueFactory = Callback { cellData: TableColumn.CellDataFeatures<Sales, String> ->
            SimpleStringProperty(
                String.format("%.2f", cellData.value.price * cellData.value.quantity)
            )
        }
        val taxColumn = TableColumn<Sales, String>("Tax 12%")
        taxColumn.cellValueFactory = Callback { cellData: TableColumn.CellDataFeatures<Sales, String> ->
            SimpleStringProperty(
                String.format("%.2f", cellData.value.price * cellData.value.quantity * 0.12)
            )
        }
        val afterTaxColumn = TableColumn<Sales, String>("After-Tax Income")
        afterTaxColumn.cellValueFactory = Callback { cellData: TableColumn.CellDataFeatures<Sales, String> ->
            SimpleStringProperty(
                String.format(
                    "%.2f",
                    cellData.value.price * cellData.value.quantity - cellData.value.price * cellData.value.quantity * 0.12
                )
            )
        }
        nameColumn.prefWidthProperty().bind(salesTable!!.widthProperty().multiply(0.3)) // 60% of the table width
        quantityColumn.prefWidthProperty().bind(salesTable!!.widthProperty().multiply(0.05)) // 40% of the table width
        typeColumn.prefWidthProperty().bind(salesTable!!.widthProperty().multiply(0.15)) // 60% of the table width
        priceColumn.prefWidthProperty().bind(salesTable!!.widthProperty().multiply(0.5 / 4)) // 40% of the table width
        grossColumn.prefWidthProperty().bind(salesTable!!.widthProperty().multiply(0.5 / 4)) // 40% of the table width
        taxColumn.prefWidthProperty().bind(salesTable!!.widthProperty().multiply(0.5 / 4)) // 40% of the table width
        afterTaxColumn.prefWidthProperty()
            .bind(salesTable!!.widthProperty().multiply(0.5 / 4)) // 40% of the table width
        salesTable!!.columns.addAll(
            listOf(
                nameColumn,
                quantityColumn,
                typeColumn,
                priceColumn,
                grossColumn,
                taxColumn,
                afterTaxColumn
            )
        )
        salesTable!!.items = salesData
        totalTable = TableView()
        totalTable!!.columnResizePolicy = TableView.UNCONSTRAINED_RESIZE_POLICY
        totalTable!!.columns.forEach(Consumer { column: TableColumn<TotalSales, *> ->
            column.isResizable = false
            column.setMinWidth(50.0)
        })
        totalTable!!.isEditable = false
        totalTable!!.fixedCellSize = 30.0
        totalTable!!.maxHeight = 80.0
        totalTable!!.minHeight = 80.0
        val totalTable1 = TableColumn<TotalSales, String>("Total Gross Income")
        totalTable1.cellValueFactory = Callback { cellData: TableColumn.CellDataFeatures<TotalSales, String> ->
            SimpleStringProperty(
                String.format("%.2f", cellData.value.gross)
            )
        }
        val totalTable2 = TableColumn<TotalSales, String>("Total Tax Deduction")
        totalTable2.cellValueFactory = Callback { cellData: TableColumn.CellDataFeatures<TotalSales, String> ->
            SimpleStringProperty(
                String.format("%.2f", cellData.value.tax)
            )
        }
        val totalTable3 = TableColumn<TotalSales, String>("Total After Tax Income")
        totalTable3.cellValueFactory = Callback { cellData: TableColumn.CellDataFeatures<TotalSales, String> ->
            SimpleStringProperty(
                String.format("%.2f", cellData.value.afterTax)
            )
        }
        totalTable1.prefWidthProperty().bind(totalTable!!.widthProperty().multiply(0.33)) // 60% of the table width
        totalTable2.prefWidthProperty().bind(totalTable!!.widthProperty().multiply(0.33)) // 40% of the table width
        totalTable3.prefWidthProperty().bind(totalTable!!.widthProperty().multiply(0.34)) // 60% of the table width
        calculateTotal()
        totalTable!!.columns.addAll(listOf(totalTable1, totalTable2, totalTable3))
        totalTable!!.items = totalSalesData
        val inputBox = HBox()
        inputBox.spacing = 10.0
        inputBox.alignment = Pos.CENTER
        quantitySalesField = TextField()
        quantitySalesField!!.promptText = "Quantity"
        quantitySalesField!!.isEditable = false
        typeSalesField = TextField()
        typeSalesField!!.promptText = "Type"
        typeSalesField!!.isEditable = false
        priceSalesField = TextField()
        priceSalesField!!.promptText = "Price"
        priceSalesField!!.isEditable = false
        itemSalesComboBox = ComboBox()
        itemSalesComboBox!!.promptText = "Item"
        itemSalesComboBox!!.onAction = EventHandler { event: ActionEvent? ->
            val selectedItemName = itemSalesComboBox!!.selectionModel.selectedItem
            val selectedItem = inventoryData.stream()
                .filter { item: Item? -> item!!.name.value == selectedItemName }
                .findFirst().orElse(null)
            if (selectedItem != null) {
                quantitySalesField!!.text = selectedItem.quantity.toString()
                typeSalesField!!.text = selectedItem.type.displayName
                priceSalesField!!.text = selectedItem.price.toString()
            }
        }
        for (entry in inventoryList) {
            itemSalesComboBox!!.items.add(entry!!.name.value)
        }
        val addButton = Button("Add New Sale")
        addButton.onAction = EventHandler { event: ActionEvent? -> addSale(stage) }
        inputBox.children.addAll(itemSalesComboBox, quantitySalesField, typeSalesField, priceSalesField, addButton)
        salesLabel = Label()
        salesLabel!!.style = "-fx-font-size: 12px;"
        salesLabel!!.alignment = Pos.CENTER
        val importButton = Button("Import")
        importButton.onAction = EventHandler { event: ActionEvent? -> importSales(stage) }
        val exportButton = Button("Export")
        exportButton.onAction = EventHandler { event: ActionEvent? -> exportSales(stage) }
        val fileBox = HBox()
        fileBox.spacing = 10.0
        fileBox.alignment = Pos.CENTER_LEFT
        fileBox.children.addAll(importButton, exportButton)
        val root = VBox()
        root.spacing = 10.0
        root.padding = Insets(10.0)
        root.alignment = Pos.TOP_CENTER
        val scene = Scene(root, 800.0, 600.0)
        root.children.addAll(
            titleLabel,
            branchAndDateBox,
            searchLabel,
            salesTable,
            totalTable,
            inputBox,
            fileBox,
            salesLabel
        )
        salesTable!!.prefHeightProperty().bind(scene.heightProperty())
        stage.scene = scene
        stage.show()
    }

    private fun calculateTotal() {
        salesTable!!.items = salesData
        if (totalSalesData.isEmpty()) {
            totalSalesData.add(TotalSales(calculateTotalGross(), calculateTotalTax(), calculateTotalAfterTax()))
        } else {
            val totalSales = totalSalesData[0]
            totalSales.gross = calculateTotalGross()
            totalSales.tax = calculateTotalTax()
            totalSales.afterTax = calculateTotalAfterTax()
        }
        totalTable!!.items = totalSalesData
    }

    // Calculate the total gross income
    private fun calculateTotalGross(): Double {
        var totalGross = 0.0
        for (sale in salesTable!!.items) {
            totalGross += sale.price * sale.quantity
        }
        return totalGross
    }

    // Calculate the total tax
    private fun calculateTotalTax(): Double {
        var totalTax = 0.0
        for (sale in salesTable!!.items) {
            totalTax += sale.price * sale.quantity * 0.12
        }
        return totalTax
    }

    // Calculate the total after-tax income
    private fun calculateTotalAfterTax(): Double {
        var totalAfterTax = 0.0
        for (sale in salesTable!!.items) {
            totalAfterTax += sale.price * sale.quantity - sale.price * sale.quantity * 0.12
        }
        return totalAfterTax
    }

    private fun addSale(stage: Stage) {
        val branch = branchField!!.text.trim { it <= ' ' }
        if (branch.isEmpty()) {
            salesLabel!!.text = "Error: Branch cannot be empty"
            return
        }
        val name = itemSalesComboBox!!.value.trim { it <= ' ' }
        if (name.isEmpty()) {
            salesLabel!!.text = "Error: Name cannot be empty"
            return
        }
        val type = ItemType.valueOf(typeSalesField!!.text.replace(" ", "_").uppercase(Locale.getDefault()))
        val quantity: Int
        try {
            quantity = quantitySalesField!!.text.trim { it <= ' ' }.toInt()
            if (quantity < 1) {
                salesLabel!!.text = "Error: Quantity must be at least 1"
                return
            }
        } catch (e: NumberFormatException) {
            salesLabel!!.text = "Error: Invalid quantity"
            return
        }
        val price = priceSalesField!!.text.trim { it <= ' ' }.toDouble()
        var quantityToRemove = 0
        val selectedItem = inventoryData.stream()
            .filter { item: Item? -> item!!.name.value == name }
            .findFirst().orElse(null)
        if (selectedItem != null) {
            val selectedQuantity = selectedItem.quantity
            val dialog = TextInputDialog(selectedQuantity.toString())
            dialog.title = "Finalise Sale"
            dialog.headerText = "Item: " + selectedItem.name.value + " - Quantity: " + selectedQuantity
            dialog.contentText = "Enter quantity of Sale:"
            val result = dialog.showAndWait()
            if (result.isPresent) {
                val input = result.get()
                try {
                    quantityToRemove = input.toInt()
                    if (quantityToRemove in 1..selectedQuantity) {
                        selectedItem.quantity = selectedQuantity - quantityToRemove
                        selectedItem.lastTransaction = LocalDateTime.now()
                        inventoryTable!!.refresh()
                        salesLabel!!.text =
                            "Removed " + quantityToRemove + " " + selectedItem.name.value + " - Quantity: " + selectedQuantity
                    } else {
                        val alert = Alert(Alert.AlertType.WARNING)
                        alert.title = "Invalid Quantity"
                        alert.headerText = null
                        alert.contentText = "Please enter a valid quantity to remove."
                        alert.showAndWait()
                        return
                    }
                } catch (e: NumberFormatException) {
                    val alert = Alert(Alert.AlertType.WARNING)
                    alert.title = "Invalid Input"
                    alert.headerText = null
                    alert.contentText = "Please enter a valid number."
                    alert.showAndWait()
                    return
                }
            }
        }
        val newSale = Sales(branch, name, quantityToRemove, type, price, LocalDateTime.now())
        salesList.add(newSale)
        salesData.add(newSale)
        salesLabel!!.text = "Added $name"
        filterInventory()
        stage.close()
        transactionManager()
    }

    // imports items to the inventory from a CSV file
    private fun importSales(stage: Stage) {
        val fileChooser = FileChooser()
        fileChooser.title = "Select Sales File"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("CSV Files", "*.csv"))
        val selectedFile = fileChooser.showOpenDialog(stage)
        salesFile = selectedFile
        if (selectedFile != null) {
            try {
                Scanner(selectedFile, StandardCharsets.UTF_8).use { scanner ->
                    val importedSales: MutableList<Sales> = ArrayList()
                    scanner.nextLine() // skip header
                    while (scanner.hasNextLine()) {
                        val line = scanner.nextLine()
                        val parts = line.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        val branch = parts[0].trim { it <= ' ' }
                        val lastTransaction = LocalDateTime.parse(parts[1].trim { it <= ' ' })
                        val name = parts[2].trim { it <= ' ' }
                        val quantity = parts[3].trim { it <= ' ' }.toInt()
                        val type = ItemType.valueOf(parts[4].trim { it <= ' ' })
                        val price = parts[5].trim { it <= ' ' }.toDouble()
                        val newSale = Sales(branch, name, quantity, type, price, lastTransaction)
                        importedSales.add(newSale)
                    }
                    salesList.addAll(importedSales)
                    salesData.addAll(importedSales)
                    salesLabel!!.text = "Imported " + importedSales.size + " items from " + selectedFile.name
                }
            } catch (e: IOException) {
                salesLabel!!.text = "Error importing items from " + selectedFile.name
            }
            salesTable!!.items = salesData
        }
        stage.close()
        transactionManager()
    }

    // exports the inventory to a CSV file
    private fun exportSales(stage: Stage) {
        val fileChooser = FileChooser()
        fileChooser.title = "Save Sales File"
        fileChooser.extensionFilters.add(FileChooser.ExtensionFilter("CSV Files", "*.csv"))
        val selectedFile = fileChooser.showSaveDialog(stage)
        if (selectedFile != null) {
            try {
                PrintWriter(FileOutputStream(selectedFile), true, StandardCharsets.UTF_8).use { writer ->
                    writer.println("Branch,Date,Item,Quantity,Type,Price")
                    for (sale in salesList) {
                        val branch = sale.branch
                        val name = sale.name
                        val lastTransaction = sale.transactionDate
                        val quantity = sale.quantity
                        val price = sale.price
                        val type = sale.type
                        writer.println("$branch,$lastTransaction,$name,$quantity,$type,$price")
                    }
                    salesLabel!!.text = "Sales exported to " + selectedFile.name
                }
            } catch (e: IOException) {
                salesLabel!!.text = "Error exporting Sales to " + selectedFile.name
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Application.launch(Main::class.java, *args)
        }
    }
}