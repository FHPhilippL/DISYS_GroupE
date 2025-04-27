module at.fhtw.bwi.disys.gui {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;


    opens at.fhtw.bwi.disys.gui to javafx.fxml, com.google.gson;
    exports at.fhtw.bwi.disys.gui;
}