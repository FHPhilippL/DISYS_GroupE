module at.fhtw.bwi.disys.gui {
    requires javafx.controls;
    requires javafx.fxml;


    opens at.fhtw.bwi.disys.gui to javafx.fxml;
    exports at.fhtw.bwi.disys.gui;
}