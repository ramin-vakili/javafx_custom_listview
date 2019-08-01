# JavaFX custom list view

Due to javafx poor performance of JavaFX listview. CustomListView is a subclass of VBox that manages creating cells, adding/removing items and etc,

# Usage
fxml:

```
        
   <AnchorPane xmlns="http://javafx.com/javafx"
       xmlns:fx="http://javafx.com/fxml"
         maxHeight="Infinity" maxWidth="Infinity"
         fx:controller="Controller">
            <CustomListView 
                fx:id="customListView"
                maxWidth="Infinity"
                maxHeight="Infinity"/>
 </AnchorPane>
                
```

Controller.java

```

```
