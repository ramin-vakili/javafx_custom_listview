import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;


public class CustomListView<T> extends VBox {

    private static final String DEFAULT_STYLE_CLASS = "rv-custom-listview";
    private ListViewCellFactory cellFactory;
    private DraggingListener dragListener;
    private ObservableList<T> items;
    @FXML
    private VBox vBoxItems;
    @FXML
    private ScrollPane scrollPane;
    private Timeline scrollTimeline = new Timeline();
    private double scrollVelocity = 0;
    private int speed = 200;
    private double itemHeightThreshold = 30.0;
    private boolean dropped = true;
    private int dragToIndex;

    public void setDragListener(DraggingListener dragListener) {
        this.dragListener = dragListener;
    }

    public CustomListView(ListViewCellFactory cellFactory) {
        this.cellFactory = cellFactory;
        initClass();
    }

    public CustomListView(ListViewCellFactory cellFactory, ObservableList<T> items) {
        this.cellFactory = cellFactory;
        initClass();
        setItems(items);
    }

    public CustomListView() {
        initClass();
    }

    private void initClass() {
        vBoxItems = new VBox();
        vBoxItems.setMaxWidth(Double.POSITIVE_INFINITY);
        vBoxItems.setMaxHeight(Double.POSITIVE_INFINITY);

        scrollPane = new ScrollPane(vBoxItems);
        scrollPane.setFitToWidth(true);

        this.getChildren().add(scrollPane);

        this.getStyleClass().add(DEFAULT_STYLE_CLASS);

        vBoxItems.setSpacing(10.0);
        vBoxItems.setPadding(new Insets(10.0));

        setupScrolling();

    }

    public void setItems(ObservableList<T> items) {
        this.items = items;
        vBoxItems.getChildren().clear();
        new Thread(() -> {
            for (int i = 0; i < items.size(); i++) {
                addNewItem(items.get(i), i);
            }
        }).start();
    }

    public void setCellFactory(ListViewCellFactory cellFactory) {
        this.cellFactory = cellFactory;
    }

    public void addNewItem(T item, int indexOf) {
        Pane cellView = cellFactory.getItem(item, indexOf);
        addWithDragging(vBoxItems, cellView);
        Platform.runLater(() -> vBoxItems.getChildren().add(cellView));
    }

    public void addNewItemSync(T item, int indexOf) {
        Pane cellView = cellFactory.getItem(item, indexOf);
        addWithDragging(vBoxItems, cellView);
        vBoxItems.getChildren().add(cellView);
    }


    public void notifyItemRemoved(T item) {
        int index = items.indexOf(item);
        items.remove(item);
        vBoxItems.getChildren().remove(index);
    }

    private void addWithDragging(final VBox root, final Pane childPane) {
        childPane.setOnDragDetected(event -> {
            int dragFrom = vBoxItems.getChildren().indexOf(childPane);
            Dragboard dragboard = childPane.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(Constants.itemIndex, dragFrom);
            dragboard.setDragView(childPane.snapshot(null, null));
            dragboard.setContent(content);
            childPane.setOpacity(0.0);
        });

        childPane.setOnDragOver(event -> {
            event.acceptTransferModes(TransferMode.MOVE);
            if (event.getGestureSource() != childPane && event.getDragboard().hasContent(Constants.itemIndex)) {
                int itemIndex = vBoxItems.getChildren().indexOf(childPane);
                event.consume();
                int draggedIndex = (int) event.getDragboard().getContent(Constants.itemIndex);
                if (draggedIndex == itemIndex) {
                    return;
                }
                dragToIndex = (draggedIndex < itemIndex) ? itemIndex - 1 : itemIndex;

                if (event.getY() < itemHeightThreshold) {
                    //Moving to top of this pane
                    childPane.setStyle("-fx-border-width: 5 0 0 0; -fx-border-color: BLACK;");
                } else if (event.getY() > (childPane.getHeight() - itemHeightThreshold)) {
                    if (dragToIndex < items.size() - 1) {
                        //Moving to bottom of this pane
                        dragToIndex++;
                        childPane.setStyle("-fx-border-width: 0 0 5 0; -fx-border-color: BLACK;");
                    }
                }

                System.out.println("dragToIndex = [" + dragToIndex + "]");
            }
        });


        childPane.setOnDragExited(event -> {
            dragToIndex = -1;
            childPane.setStyle(null);
        });


        childPane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasContent(Constants.itemIndex)) {
                success = true;
                if (dragToIndex > -1) {
                    int dragFromIndex = (int) event.getDragboard().getContent(Constants.itemIndex);
                    dragListener.onItemMove(dragFromIndex, dragToIndex);
                    dropped = true;
                    notifyItemReordered(dragFromIndex, dragToIndex);
                }
            }
            event.setDropCompleted(success);
            event.consume();

        });

        childPane.setOnDragDone(event -> {
            childPane.setOpacity(1.0);
            dragToIndex = -1;
            event.consume();
        });

    }

    public void notifyItemReordered(final int indexOfDraggingNode,
                                    final int indexOfDropTarget) {
        if (indexOfDraggingNode >= 0 && indexOfDropTarget >= 0) {
            final Node node = vBoxItems.getChildren().remove(indexOfDraggingNode);
            vBoxItems.getChildren().add(indexOfDropTarget, node);
        }
    }

    //To auto scroll when mouse drag is close to bottom or above
    private void setupScrolling() {
        scrollTimeline.setCycleCount(Timeline.INDEFINITE);
        scrollTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(20), (ActionEvent) -> {
            dragScroll();
        }));

        scrollPane.setOnDragExited((DragEvent event) -> {

            if (event.getY() > 0) {
                scrollVelocity = 1.0 / speed;
            } else {
                scrollVelocity = -1.0 / speed;
            }
            if (!dropped) {
                scrollTimeline.play();
            }

        });

        scrollPane.setOnDragEntered(event -> {
            scrollTimeline.stop();
            dropped = false;
        });
        scrollPane.setOnDragDone(event -> {
            System.out.print("test");
            scrollTimeline.stop();
        });
        scrollPane.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            ((VBox) scrollPane.getContent()).getChildren().add(new Label(db.getString()));
            scrollTimeline.stop();
            event.setDropCompleted(true);
            dropped = true;


        });

        scrollPane.setOnDragOver((DragEvent event) -> {
            event.acceptTransferModes(TransferMode.MOVE);
        });


        scrollPane.setOnScroll((ScrollEvent event) -> {
            scrollTimeline.stop();
        });

        scrollPane.setOnMouseClicked((MouseEvent) -> {
            System.out.println(scrollTimeline.getStatus());

        });

    }

    private void dragScroll() {
        ScrollBar sb = getVerticalScrollbar();
        if (sb != null) {
            double newValue = sb.getValue() + scrollVelocity;
            newValue = Math.min(newValue, 1.0);
            newValue = Math.max(newValue, 0.0);
            sb.setValue(newValue);
        }
    }

    private ScrollBar getVerticalScrollbar() {
        ScrollBar result = null;
        for (Node n : scrollPane.lookupAll(".scroll-bar")) {
            if (n instanceof ScrollBar) {
                ScrollBar bar = (ScrollBar) n;
                if (bar.getOrientation().equals(Orientation.VERTICAL)) {
                    result = bar;
                }
            }
        }
        return result;
    }

    public VBox getListView() {
        return vBoxItems;
    }

    public interface ListViewCellFactory<T> {
        Pane getItem(T item, int position);
    }

    public interface DraggingListener {
        void onItemMove(int from, int to);
    }

}
