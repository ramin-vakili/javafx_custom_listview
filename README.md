# JavaFX custom list view

Due to javafx poor performance of JavaFX listview. CustomListView is a subclass of VBox that manages creating cells, adding/removing items and etc,

# Usage
fxml:

```fxml
        
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

```java
@FXML
public CustomListView<Mark> customListView;
ObservableList<ItemModelClass> itemsList;
...

customListView.setCellFactory((CustomListView.ListViewCellFactory<ItemModelClass>) (item, position) -> {
            ItemCellController cell = new ItemCellController(itemsList, this, ResourceBundle);
            return cell.getItemPane(item);
        });
        
customListView.setDragListener(this::onItemMove);

```

Inside `onItemMove` that you assign as DragListener you can for example do order changes in your persistent storage, API, etc.

```Java
private void onItemMove(int fromPos, int toPos){
     // Save the changes in persistent storage (DB, ...)
}
```
