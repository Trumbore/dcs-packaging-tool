/*
  * Copyright 2014 Johns Hopkins University
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package org.dataconservancy.packaging.gui.view.impl;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.dataconservancy.dcs.util.DisciplineLoadingService;
import org.dataconservancy.packaging.gui.Configuration;
import org.dataconservancy.packaging.gui.Errors;
import org.dataconservancy.packaging.gui.Help;
import org.dataconservancy.packaging.gui.InternalProperties;
import org.dataconservancy.packaging.gui.Labels.LabelKey;
import org.dataconservancy.packaging.gui.Messages;
import org.dataconservancy.packaging.gui.TextFactory;
import org.dataconservancy.packaging.gui.presenter.EditPackageContentsPresenter;
import org.dataconservancy.packaging.gui.util.PackageToolPopup;
import org.dataconservancy.packaging.gui.util.ProfilePropertyBox;
import org.dataconservancy.packaging.gui.util.ProgressDialogPopup;
import org.dataconservancy.packaging.gui.util.UserDefinedPropertyBox;
import org.dataconservancy.packaging.gui.view.EditPackageContentsView;
import org.dataconservancy.packaging.tool.api.IPMService;
import org.dataconservancy.packaging.tool.api.support.NodeComparison;
import org.dataconservancy.packaging.tool.model.dprofile.FileAssociation;
import org.dataconservancy.packaging.tool.model.dprofile.NodeTransform;
import org.dataconservancy.packaging.tool.model.dprofile.NodeType;
import org.dataconservancy.packaging.tool.model.dprofile.Property;
import org.dataconservancy.packaging.tool.model.dprofile.PropertyConstraint;
import org.dataconservancy.packaging.tool.model.dprofile.PropertyType;
import org.dataconservancy.packaging.tool.model.ipm.Node;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * Implementation of the view that displays the package description tree, and the controls for applying inherited metadata.
 */
public class EditPackageContentsViewImpl extends BaseViewImpl<EditPackageContentsPresenter> implements EditPackageContentsView {

    private TreeTableView<Node> nodeTree;

    private Stage nodePropertiesWindow;
    private Scene nodePropertiesScene;
    private Node popupNode;
    private IPMService ipmService;
    private Map<Node, NodeComparison> refreshResult;

    //Warning popup and controls
    private PackageToolPopup warningPopup;
    private Button warningPopupPositiveButton;
    private Button warningPopupNegativeButton;
    private CheckBox hideFutureWarningPopupCheckBox;

    private Button reenableWarningsButton;

    //Metadata Inheritance Controls
    private Map<PropertyType, CheckBox> metadataInheritanceButtonMap;

    //File chooser for where to save package description. 
    private FileChooser packageStateFileChooser;

    //Full Path checkbox
    private CheckBox fullPath;

    //Whether to show ignored Artifacts
    private CheckBox showIgnored;

    //Controls that are displayed in the package artifact popup.
    private Hyperlink cancelPopupLink;
    private Button applyPopupButton;

    private InternalProperties internalProperties;

    private Preferences preferences;
    private NodePropertyWindowBuilder windowBuilder;

    private PackageToolPopup refreshPopup;
    private Button refreshPopupPositiveButton;
    private Button refreshPopupNegativeButton;

    private ProgressDialogPopup validationProgressPopup;

    private Set<URI> nodesMissingFiles;
    private ContextMenu nodeContextMenu;

    public EditPackageContentsViewImpl (final InternalProperties internalProperties) {
        super();
        this.internalProperties = internalProperties;

        nodesMissingFiles = new HashSet<>();

        //Sets the text of the footer controls.
        getContinueButton().setText(TextFactory.getText(LabelKey.SAVE_AND_CONTINUE_BUTTON));
        getCancelLink().setText(TextFactory.getText(LabelKey.BACK_LINK));
        getSaveButton().setText(TextFactory.getText(LabelKey.SAVE_BUTTON));
        getSaveButton().setVisible(true);
        VBox content = new VBox();

        content.getStyleClass().add(EDIT_PACKAGE_CONTENTS_VIEW_CLASS);
        setCenter(content);

        //This code handles the copy/paste functionality that allows you to copy the id of a domain object into a user defined relationship target
        content.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.C && event.isShortcutDown()) {
                if (nodeTree != null) {
                    final Clipboard clipboard = Clipboard.getSystemClipboard();
                    final ClipboardContent clipboardContent = new ClipboardContent();

                    String selectedNodeIds = "";
                    for (TreeItem<Node> selectedNode : nodeTree.getSelectionModel().getSelectedItems()) {
                        selectedNodeIds += selectedNode.getValue().getDomainObject() + "\n";
                    }

                    clipboardContent.putString(selectedNodeIds);
                    clipboard.setContent(clipboardContent);
                }
            }
        });

        if (Platform.isFxApplicationThread()) {
            nodePropertiesWindow = new Stage();
            nodePropertiesWindow.setMinWidth(540);
            nodePropertiesWindow.setMinHeight(500);
        }

        //This preference stores whether or not we're going to show the property loss warning, this warning can be disabled by the user
        preferences = Preferences.userRoot().node(internalProperties.get(InternalProperties.InternalPropertyKey.PREFERENCES_NODE_NAME));
        boolean hideWarningPopup = preferences.getBoolean(internalProperties.get(InternalProperties.InternalPropertyKey.HIDE_PROPERTY_WARNING_PREFERENCE), false);

        HBox buttonBar = new HBox(24);
        buttonBar.setAlignment(Pos.TOP_RIGHT);
        reenableWarningsButton = new Button(TextFactory.getText(LabelKey.RENABLE_PROPERTY_WARNINGS_BUTTON));
        reenableWarningsButton.setVisible(hideWarningPopup);
        reenableWarningsButton.setPrefWidth(23 * rem);
        buttonBar.getChildren().add(reenableWarningsButton);
        content.getChildren().add(buttonBar);

        //Creates the file chooser that's used to save the package description to a file.
        packageStateFileChooser = new FileChooser();
        packageStateFileChooser.setTitle(TextFactory.getText(LabelKey.PACKAGE_STATE_FILE_CHOOSER_KEY));
        packageStateFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Package Description (*.json)", "*.json"));
        packageStateFileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files (*.*)", "*.*"));

        //Toggles whether the full paths should be displayed in the package artifact tree. 
        fullPath = new CheckBox(TextFactory.getText(LabelKey.SHOW_FULL_PATHS));
        fullPath.selectedProperty().addListener((ov, old_val, new_val) -> {
            presenter.rebuildTreeView();
        });

        showIgnored = new CheckBox(TextFactory.getText(LabelKey.SHOW_IGNORED));
        showIgnored.selectedProperty().setValue(true);
        showIgnored.selectedProperty().addListener((observableValue, aBoolean, aBoolean2) -> {
            presenter.rebuildTreeView();
        });

        if(Platform.isFxApplicationThread()){
            Tooltip t = new Tooltip(TextFactory.getText(LabelKey.SHOW_FULL_PATHS_TIP));
            t.setPrefWidth(300);
            t.setWrapText(true);
            fullPath.setTooltip(t);
        }
        if(Platform.isFxApplicationThread()){
            Tooltip t = new Tooltip(TextFactory.getText(LabelKey.SHOW_IGNORED_TIP));
            t.setPrefWidth(300);
            t.setWrapText(true);
            showIgnored.setTooltip(t);
        }

        content.getChildren().add(fullPath);
        content.getChildren().add(showIgnored);

        //The main element of the view a tree of all the package artifacts.
        nodeTree = new TreeTableView<>();

        nodeTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //disable column sorting in the view
        nodeTree.setSortPolicy(treeTableView -> false);

        //set up the columns for the artifact, its type and the options control
        TreeTableColumn<Node, HBox> packageResourceColumn = new TreeTableColumn<>(TextFactory.getText(LabelKey.PACKAGE_RESOURCE_LABEL));
        packageResourceColumn.setResizable(false);
        TreeTableColumn<Node, Label> typeColumn = new TreeTableColumn<>(TextFactory.getText(LabelKey.TYPE_LABEL));
        typeColumn.setResizable(false);
        TreeTableColumn<Node, Label> actionColumn = new TreeTableColumn<>(TextFactory.getText(LabelKey.ACTIONS_LABEL));
        actionColumn.setResizable(false);

        // make the last two columns fixed width, and the first column variable, so that increasing window width widens the first column
        typeColumn.setPrefWidth(100); // make wide enough so that any displayed text will not truncate
        actionColumn.setPrefWidth(100); //make wide enough to comfortably fit image and vertical scroll bar
        
        // Dynamically resize resource column to take up space, add 2 here to get rid of horizontal scroll bar
        packageResourceColumn.prefWidthProperty().bind(nodeTree.widthProperty().subtract(typeColumn.getWidth() + actionColumn.getWidth() + 2));

        //For these cell factories p.getValue returns the TreeItem<Node> p.getValue.getValue returns the node.
        packageResourceColumn.setCellValueFactory(p -> {
            Node packageNode = p.getValue().getValue();

            HBox hbox = new HBox(3);

            ImageView exclaimImage = new ImageView("/images/orange_exclamation.png");
            exclaimImage.setFitHeight(20);
            exclaimImage.setFitWidth(5);

            Label exclaimLabel = new Label();
            exclaimLabel.setGraphic(exclaimImage);

            Tooltip exclaimTooltip = new Tooltip(TextFactory.getText(LabelKey.FILE_MISSING_TIP));
            exclaimTooltip.setPrefWidth(300);
            exclaimTooltip.setWrapText(true);
            exclaimTooltip.setFont(Font.font(12));
            Tooltip.install(exclaimLabel, exclaimTooltip);

            if (packageNode.getFileInfo() != null && !ipmService.checkFileInfoIsAccessible(packageNode) && !packageNode.isIgnored()) {
                hbox.getChildren().add(exclaimLabel);
                nodesMissingFiles.add(packageNode.getIdentifier());
            } else if (nodesMissingFiles.contains(packageNode.getIdentifier())){
                nodesMissingFiles.remove(packageNode.getIdentifier());
            }

            Label viewLabel = new Label();
            viewLabel.setPrefWidth(packageResourceColumn.getWidth());
            viewLabel.setTextOverrun(OverrunStyle.CENTER_ELLIPSIS);

            String labelText;
            if (packageNode.getFileInfo() != null) {
                labelText = packageNode.getFileInfo().getName();
                if (getFullPathCheckBox().selectedProperty().getValue() && packageNode.getFileInfo().getLocation() != null ) {
                    labelText = packageNode.getFileInfo().getLocation().toString();
                }
                viewLabel.setText(labelText);
            } else {
                viewLabel.setText("[" + TextFactory.getText(LabelKey.NO_BACKING_FILE_LABEL) + "]");
                viewLabel.getStyleClass().add(NO_FILE_ENTITY);
            }

            Tooltip t = new Tooltip(viewLabel.getText());
            t.setPrefWidth(300);
            t.setWrapText(true);
            viewLabel.setTooltip(t);

            hbox.getChildren().add(viewLabel);



            return new ReadOnlyObjectWrapper<>(hbox);
        });

        typeColumn.setCellValueFactory(p -> {
            Label typeLabel = new Label(TextFactory.getText(LabelKey.UNASSIGNED_LABEL));
            typeLabel.setPrefWidth(typeColumn.getWidth());

            if (p.getValue().getValue().getNodeType() != null) {
                String type = p.getValue().getValue().getNodeType().getLabel();
                typeLabel.setText(type);
            }
            return new ReadOnlyObjectWrapper<>(typeLabel);
        });

        actionColumn.setCellFactory(new Callback<TreeTableColumn<Node, Label>, TreeTableCell<Node, Label>>() {
            @Override
            public TreeTableCell<Node, Label> call(TreeTableColumn<Node, Label> param) {
                return new TreeTableCell<Node, Label>() {

                    @Override
                    public void updateItem(Label item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            Button optionsLabel = new Button();
                            optionsLabel.setPrefHeight(25);
                            optionsLabel.setPrefWidth(50);
                            optionsLabel.getStyleClass().clear();
                            optionsLabel.getStyleClass().add(EDIT_PACKAGE_TREE_BUTTON);
                            optionsLabel.setAlignment(Pos.CENTER);
                            ImageView image = new ImageView();
                            image.setFitHeight(24);
                            image.setFitWidth(6);
                            image.getStyleClass().add(ARROWS_IMAGE);
                            optionsLabel.setGraphic(image);

                            //When the options label is clicked show the context menu.
                            optionsLabel.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, e -> {
                                if (e.getButton() == MouseButton.PRIMARY) {
                                    if (nodeContextMenu == null) {
                                        nodeContextMenu = new ContextMenu();
                                    }
                                    nodeContextMenu.getItems().clear();

                                    //Hide the old context menu
                                    if(nodeContextMenu.isShowing()) {
                                        nodeContextMenu.hide();
                                    }

                                    nodeContextMenu.setAutoHide(true);

                                    ObservableList<TreeItem<Node>> selectedItems = nodeTree.getSelectionModel().getSelectedItems();

                                    //If no rows are selected or the current row isn't selected add the current row to the selection
                                    //This is to help keep behavior consistent
                                    if (selectedItems == null || selectedItems.isEmpty() || !selectedItems.contains(nodeTree.getTreeItem(getIndex()))) {
                                        //If ctrl is pressed add the row otherwise, replace selection
                                        if (e.isControlDown()) {
                                            nodeTree.getSelectionModel().select(getIndex());
                                        } else {
                                            nodeTree.getSelectionModel().clearAndSelect(getIndex());
                                        }

                                        selectedItems = nodeTree.getSelectionModel().getSelectedItems();
                                    }

                                    //Loop through the selected nodes and determine if they're all ignored, and the possible available node transforms.
                                    boolean allIgnored = true;
                                    boolean hasIgnored = false;
                                    List<Node> selectedNodes = new ArrayList<>();
                                    for (TreeItem<Node> selectedNode : selectedItems) {
                                        if (!selectedNode.getValue().isIgnored()) {
                                            allIgnored = false;
                                        } else {
                                            hasIgnored = true;
                                        }
                                        selectedNodes.add(selectedNode.getValue());
                                    }

                                    List<NodeTransform> possibleTransforms = new ArrayList<>();
                                    if (!hasIgnored) {
                                        possibleTransforms = presenter.getController().getDomainProfileService().getNodeTransforms(selectedNodes);
                                    }

                                    //If all items are ignored we give the option to unignore otherwise we give the option to ignore
                                    if (allIgnored) {
                                        nodeContextMenu.getItems().add(createIgnoreMenuItem(selectedItems, true));
                                    } else {
                                        nodeContextMenu.getItems().addAll(createMenuItemList(selectedItems, possibleTransforms, optionsLabel, hasIgnored));
                                        //optionsLabel.setContextMenu(contextMenu);
                                    }

                                    nodeContextMenu.show(optionsLabel, e.getScreenX(), e.getScreenY());
                                }
                            });
                            setGraphic(optionsLabel);
                            setAlignment(Pos.CENTER);
                            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                        }
                    }
                };
            }
        });

        nodeTree.getColumns().add(packageResourceColumn);
        nodeTree.getColumns().add(typeColumn);
        nodeTree.getColumns().add(actionColumn);

        //set up row factory to allow for a little alternate row styling for ignored package artifacts
        nodeTree.setRowFactory(new Callback<TreeTableView<Node>, TreeTableRow<Node>>() {
            @Override
             public TreeTableRow<Node> call(TreeTableView<Node> ttv) {
                TreeTableRow<Node> nodeTreeTableRow = new TreeTableRow<Node>() {
                    @Override
                    public void updateItem(Node packageNode, boolean empty) {
                        super.updateItem(packageNode, empty);
                        if (packageNode != null && packageNode.isIgnored()) {
                            getStyleClass().add(PACKAGE_CONTENT_TREE_IGNORE_ROW);
                        } else {
                            getStyleClass().removeAll(PACKAGE_CONTENT_TREE_IGNORE_ROW);
                        }
                    }
                };

                //If the user drags over a row initiate the drag and drop code to copy the value of the domain object associated with the row.
                nodeTreeTableRow.setOnDragDetected(event -> {
                    //If drag was detected start drag and drop with the copy mode.
                    Dragboard db = nodeTreeTableRow.startDragAndDrop(TransferMode.COPY);

                    //Put the id of the domain object on the clipboard to copy.
                    ClipboardContent content1 = new ClipboardContent();
                    content1.putString(nodeTreeTableRow.getTreeItem().getValue().getDomainObject().toString());
                    db.setContent(content1);

                    event.consume();

                });

                //If the user clicks on a row update the content of the node property window if it's open.
                nodeTreeTableRow.setOnMouseClicked(event -> {
                    if (nodePropertiesWindow != null && nodePropertiesWindow.isShowing()) {
                        //If new value is null it means the node has been removed, so close the window
                        presenter.saveCurrentNode();
                        showNodePropertiesWindow(nodeTreeTableRow.getTreeItem().getValue(), null);
                    }

                });
                return nodeTreeTableRow;
            }
        });

        content.getChildren().add(nodeTree);

        ImageView missingFileIndicator = new ImageView("/images/orange_exclamation.png");
        missingFileIndicator.setFitHeight(20);
        missingFileIndicator.setFitWidth(5);

        ImageView missingPropertyIndicator = new ImageView("/images/red_exclamation.png");
        missingPropertyIndicator.setFitHeight(20);
        missingPropertyIndicator.setFitWidth(5);

        VBox legend = new VBox(6);
        Label missingFile = new Label(TextFactory.getText(LabelKey.MISSING_FILE_LEGEND));
        missingFile.setGraphic(missingFileIndicator);
        legend.getChildren().add(missingFile);

        content.getChildren().add(legend);
        VBox.setVgrow(nodeTree, Priority.ALWAYS);

        //Controls for the package artifact popup
        cancelPopupLink = new Hyperlink(TextFactory.getText(LabelKey.CANCEL_BUTTON));
        applyPopupButton = new Button(TextFactory.getText(LabelKey.APPLY_BUTTON));

        //Controls for the validation error popup.
        warningPopupPositiveButton = new Button(TextFactory.getText(LabelKey.OK_BUTTON));
        warningPopupPositiveButton.getStyleClass().add(CLICKABLE);
        warningPopupNegativeButton = new Button(TextFactory.getText(LabelKey.CANCEL_BUTTON));
        warningPopupNegativeButton.getStyleClass().add(CLICKABLE);
        hideFutureWarningPopupCheckBox = new CheckBox(TextFactory.getText(LabelKey.DONT_SHOW_WARNING_AGAIN_CHECKBOX));

        //Instantiating metadata inheritance button map
        metadataInheritanceButtonMap = new HashMap<>();

        // controls for the refresh popup
        refreshPopupPositiveButton = new Button(TextFactory.getText(LabelKey.ACCEPT_BUTTON));
        refreshPopupPositiveButton.getStyleClass().add(CLICKABLE);
        refreshPopupNegativeButton = new Button(TextFactory.getText(LabelKey.REJECT_BUTTON));
        refreshPopupNegativeButton.getStyleClass().add(CLICKABLE);
    }

    @Override
    public TreeItem<Node> getRoot() {
        return nodeTree.getRoot();
    }

    @Override
    public TreeTableView<Node> getArtifactTreeView() {
        return nodeTree;
    }

    @Override
    public CheckBox getFullPathCheckBox(){
        return fullPath;
    }

    @Override
    public String getHelpText() {
        return TextFactory.getText(Help.HelpKey.EDIT_PACKAGE_CONTENTS_HELP);
    }

    //Create the menu items for the selected rows in the tree view.
    private List<MenuItem> createMenuItemList(final ObservableList<TreeItem<Node>> selectedItems, List<NodeTransform> nodeTransforms, final Button label, boolean hasIgnored){
        List<MenuItem> itemList = new ArrayList<>();

        //If there is only one item selected we display the properties, add, refresh and remap options
        if (selectedItems.size() == 1) {
            final Node packageNode = selectedItems.get(0).getValue();

            //Create a menu item that will show the package artifacts popup.
            MenuItem propertiesItem = new MenuItem(TextFactory.getText(LabelKey.PROPERTIES_LABEL));
            itemList.add(propertiesItem);
            propertiesItem.setOnAction(actionEvent -> {
                showNodePropertiesWindow(packageNode, label);
                nodeTree.getSelectionModel().select(selectedItems.get(0));
            });
            List<NodeType> childNodeTypes = presenter.getPossibleChildTypes(packageNode);

            boolean canHaveFileChild = false;
            boolean canHaveDirectoryChild = false;

            for (NodeType childNodeType : childNodeTypes) {
                if (childNodeType.getFileAssociation() == FileAssociation.DIRECTORY) {
                    canHaveDirectoryChild = true;
                } else {
                    canHaveFileChild = true;
                }

                if (canHaveDirectoryChild && canHaveFileChild) {
                    break;
                }
            }

            if (canHaveFileChild) {
                //Create a menu item that will allow the user to pick a file.
                MenuItem addFileItem = new MenuItem(TextFactory.getText(LabelKey.ADD_FILE_ITEM_LABEL));
                itemList.add(addFileItem);
                addFileItem.setOnAction(event -> {
                    File file = presenter.getController().showOpenFileDialog(new FileChooser());
                    if (file != null) {
                        presenter.addToTree(packageNode, file.toPath());
                    }
                });
            }

            if (canHaveDirectoryChild) {
                //Create a menu item that will allow the user to pick a folder.
                MenuItem addDirItem = new MenuItem(TextFactory.getText(LabelKey.ADD_FOLDER_ITEM_LABEL));
                itemList.add(addDirItem);
                addDirItem.setOnAction(event -> {
                    File file = presenter.getController().showOpenDirectoryDialog(new DirectoryChooser());
                    if (file != null) {
                        presenter.addToTree(packageNode, file.toPath());
                    }
                });
            }

            //If backing file is accessible allow the user to refresh
            if (packageNode.getFileInfo() != null && ipmService.checkFileInfoIsAccessible(packageNode)) {
                MenuItem refreshItem = new MenuItem(TextFactory.getText(LabelKey.REFRESH_ITEM_LABEL));
                itemList.add(refreshItem);
                refreshItem.setOnAction(event -> {
                    refreshResult = presenter.refreshTreeContent(packageNode);
                    showRefreshResultsPopup();
                });
            }

            //If the backing file system entity is not available give the option to remap
            if (packageNode.getFileInfo() != null && !ipmService.checkFileInfoIsAccessible(packageNode)) {
                MenuItem remapFileItem = new MenuItem(TextFactory.getText(LabelKey.REMAP_ITEM_LABEL));
                itemList.add(remapFileItem);
                remapFileItem.setOnAction(event -> {
                    File file = null;
                    if (packageNode.getFileInfo().isFile()) {
                        file = presenter.getController().showOpenFileDialog(new FileChooser());
                    } else {
                        file = presenter.getController().showOpenDirectoryDialog(new DirectoryChooser());
                    }
                    if (file != null) {
                        //Remap the node to the new file and check all it's descendants to see if they can be remapped also.
                        try {
                            ipmService.remapNode(packageNode, file.toPath());
                        } catch (IOException e) {
                            presenter.showError(TextFactory.getText(Errors.ErrorKey.IO_CREATE_ERROR));
                        }

                        //Redisplay the tree to update the warnings
                        presenter.displayPackageTree();
                    }

                });
            }

            SeparatorMenuItem separator =  new SeparatorMenuItem();
            separator.setStyle("-fx-color: #FFFFFF");
            itemList.add(separator);
            //if there is an error, clear it
            presenter.clearError();
        }


        //Create a menu item for each available node transform
        if (!hasIgnored && nodeTransforms != null && nodeTransforms.size() > 0) {

            for (final NodeTransform transform : nodeTransforms) {
                final List<PropertyType> invalidProperties = new ArrayList<>();

                List<PropertyType> newTypeProperties = transform.getResultNodeType().getPropertyConstraints().stream().map(PropertyConstraint::getPropertyType).collect(Collectors.toList());
                invalidProperties.addAll(selectedItems.get(0).getValue().getNodeType().getPropertyConstraints().stream().filter(newTypeConstraint -> !newTypeProperties.contains(newTypeConstraint.getPropertyType())).map(PropertyConstraint::getPropertyType).collect(Collectors.toList()));

                MenuItem item = new MenuItem(transform.getLabel());
                itemList.add(item);

                if (!invalidProperties.isEmpty()) {
                    ImageView invalidImage = new ImageView("/images/yellow_exclamation.png");
                    invalidImage.setFitWidth(8);
                    invalidImage.setFitHeight(24);
                    item.setGraphic(invalidImage);
                }

                item.setOnAction(actionEvent -> {
                    boolean hideWarningPopup = preferences.getBoolean(internalProperties.get(InternalProperties.InternalPropertyKey.HIDE_PROPERTY_WARNING_PREFERENCE), false);

                    List<Node> selectedNodes = selectedItems.stream().map(TreeItem::getValue).collect(Collectors.toList());

                    if (!invalidProperties.isEmpty() && !hideWarningPopup) {

                        showWarningPopup(TextFactory.getText(
                                Errors.ErrorKey.PROPERTY_LOSS_WARNING), TextFactory.format(Messages.MessageKey.WARNING_INVALID_PROPERTY, transform.getResultNodeType().getLabel(), formatInvalidProperties(invalidProperties)), true, true);
                        getWarningPopupNegativeButton().setOnAction(actionEvent1 -> {
                            getWarningPopup().hide();
                            preferences.putBoolean(internalProperties.get(InternalProperties.InternalPropertyKey.HIDE_PROPERTY_WARNING_PREFERENCE), hideFutureWarningPopupCheckBox.selectedProperty().getValue());
                        });

                        getWarningPopupPositiveButton().setOnAction(actionEvent1 -> {
                            getWarningPopup().hide();
                            presenter.changeType(selectedNodes, transform);
                            preferences.putBoolean(internalProperties.get(InternalProperties.InternalPropertyKey.HIDE_PROPERTY_WARNING_PREFERENCE), hideFutureWarningPopupCheckBox.selectedProperty().getValue());
                        });
                    } else {
                        presenter.changeType(selectedNodes, transform);
                    }

                });
            }
            SeparatorMenuItem separator =  new SeparatorMenuItem();
            separator.setStyle("-fx-color: #FFFFFF");
            itemList.add(separator);
        }

        itemList.add(createIgnoreMenuItem(selectedItems, false));

       return itemList;
    }

    //Creates a string that lists all the invalid properties in single line list.
    private String formatInvalidProperties(List<PropertyType> invalidProperties) {
        String invalidPropertyString = "";

        for (PropertyType property : invalidProperties) {
            invalidPropertyString += property.getLabel() + "\n";
        }

        return invalidPropertyString;
    }

    private MenuItem createIgnoreMenuItem(final ObservableList<TreeItem<Node>> treeItems, boolean ignore) {
        final CheckMenuItem ignoreCheck = new CheckMenuItem(TextFactory.getText(LabelKey.IGNORE_CHECKBOX));
        ignoreCheck.getStyleClass().add(EDIT_PACKAGE_TREE_MENU);

        ignoreCheck.setSelected(ignore);
        ignoreCheck.setOnAction(event -> presenter.toggleItemIgnore(treeItems, ignoreCheck.isSelected()));

        return ignoreCheck;
    }

    @Override
    public void showNodePropertiesWindow(Node packageNode, javafx.scene.Node anchorNode) {

        if (nodePropertiesWindow != null) {
            popupNode = packageNode;

            List<Property> userDefinedProperties = null;
            if (presenter.getController().getPackageState().getUserSpecifiedProperties() != null) {
                userDefinedProperties = presenter.getController().getPackageState().getUserSpecifiedProperties().get(packageNode.getIdentifier());
            }

            Pane propertiesPane = windowBuilder.buildArtifactPropertiesLayout(packageNode, metadataInheritanceButtonMap, presenter.getController().getDomainProfileService(),
                                                                              userDefinedProperties);

            if (nodePropertiesScene == null) {
                nodePropertiesScene = new Scene(propertiesPane, 540, 500);
            } else {
                nodePropertiesScene.setRoot(propertiesPane);
            }

            String windowTitle = "";
            if (packageNode.getFileInfo() != null) {
                windowTitle += packageNode.getFileInfo().getName() + " ";
            }

            windowTitle += "Properties";
            nodePropertiesWindow.setTitle(windowTitle);
            nodePropertiesWindow.setScene(nodePropertiesScene);

            if (!nodePropertiesWindow.isShowing()) {
                if (anchorNode != null) {
                    Point2D point = anchorNode.localToScene(0.0, 0.0);
                    double x = getScene().getWindow().getX() + point.getX();
                    double y = getScene().getWindow().getY() + point.getY();

                    //X and Y are now the location of the menu, offset slightly in from that this ensures if the app is full screen we don't open right on the edge
                    x -= 500;
                    y -= 100;

                    nodePropertiesWindow.setX(x);
                    nodePropertiesWindow.setY(y);
                }
                nodePropertiesWindow.show();
            }
        }
    }

    public Stage getNodePropertiesWindow() {
        return nodePropertiesWindow;
    }

    @Override
    public Button getReenableWarningsButton() {
        return reenableWarningsButton;
    }

    public void setIpmService(IPMService ipmService) {
        this.ipmService = ipmService;
    }

    @Override
    public Hyperlink getCancelPopupHyperlink() {
        return cancelPopupLink;
    }

    @Override
    public Button getApplyPopupButton() {
        return applyPopupButton;
    }

    @Override
    public List<ProfilePropertyBox> getProfilePropertyBoxes() {
        return windowBuilder.getNodePropertyBoxes();
    }

    @Override
    public List<UserDefinedPropertyBox> getUserDefinedPropertyBoxes() {
        return windowBuilder.getUserDefinedPropertyBoxes();
    }

    public Node getPopupNode() {
        return popupNode;
    }

    @Override
    public PackageToolPopup getWarningPopup() {
        return warningPopup;
    }

    @Override
    public ProgressDialogPopup getValidationProgressPopup() {
        if (validationProgressPopup == null) {
            validationProgressPopup = new ProgressDialogPopup(true);
            validationProgressPopup.setTitleText(TextFactory.getText(LabelKey.PROPERTY_VALIDATION_PROGRESS_LABEL));

            if (getScene() != null && getScene().getWindow() != null) {
                double x = getScene().getWindow().getX() + getScene().getWidth()/2.0 - 150;
                double y = getScene().getWindow().getY() + getScene().getHeight()/2.0 - 150;
                validationProgressPopup.setOwner(getScene().getWindow());
                validationProgressPopup.show(x, y);
                validationProgressPopup.hide();

                //Get the content width and height to property center the popup.
                x = getScene().getWindow().getX() + getScene().getWidth()/2.0 - validationProgressPopup.getWidth()/2.0;
                y = getScene().getWindow().getY() + getScene().getHeight()/2.0 - validationProgressPopup.getHeight()/2.0;
                validationProgressPopup.setOwner(getScene().getWindow());
                validationProgressPopup.show(x, y);
            }
        }

        return validationProgressPopup;
    }

    @Override
    public void showWarningPopup(String title, String errorMessage, boolean allowNegative, boolean allowFutureHide) {
        if (warningPopup == null) {
            warningPopup = new PackageToolPopup();
        }

        warningPopup.setTitleText(title);

        VBox content = new VBox(48);
        content.setPrefWidth(300);
        Label errorMessageLabel = new Label(errorMessage);
        errorMessageLabel.setWrapText(true);
        content.getChildren().add(errorMessageLabel);

        if (allowFutureHide) {
            content.getChildren().add(hideFutureWarningPopupCheckBox);
        }

        HBox controls = new HBox(16);
        controls.setAlignment(Pos.BOTTOM_RIGHT);
        if (allowNegative) {
            controls.getChildren().add(warningPopupNegativeButton);
        }
        controls.getChildren().add(warningPopupPositiveButton);
        content.getChildren().add(controls);

        warningPopup.setContent(content);

        if (Platform.isFxApplicationThread()) {
            //Quickly display the popup so we can measure the content
            double x =
                getScene().getWindow().getX() + getScene().getWidth() / 2.0 -
                    150;
            double y =
                getScene().getWindow().getY() + getScene().getHeight() / 2.0 -
                    150;
            warningPopup.setOwner(getScene().getWindow());
            warningPopup.show(x, y);
            warningPopup.hide();

            //Get the content width and height to property center the popup.
            x = getScene().getWindow().getX() + getScene().getWidth() / 2.0 -
                content.getWidth() / 2.0;
            y = getScene().getWindow().getY() + getScene().getHeight() / 2.0 -
                content.getHeight() / 2.0;
            warningPopup.setOwner(getScene().getWindow());
            warningPopup.show(x, y);
        }

    }

    @Override
    public Button getWarningPopupPositiveButton() {
        return warningPopupPositiveButton;
    }

    private void showRefreshResultsPopup() {
        if (refreshPopup == null) {
            refreshPopup = new PackageToolPopup();
        }

        refreshPopup.setTitleText(TextFactory.getText(LabelKey.DETECTED_CHANGES_LABEL));

        VBox content = new VBox(16);
        content.setPrefWidth(600);

        int addCount = 0;
        int deleteCount = 0;
        int updateCount = 0;

        ObservableList<ComparisonResult> resultTableData = FXCollections.observableArrayList();
        for (Node node : refreshResult.keySet()) {
            resultTableData.add(new ComparisonResult(refreshResult.get(node), node));
            switch (refreshResult.get(node).getStatus()) {
                case ADDED:
                    addCount++;
                    break;
                case DELETED:
                    deleteCount++;
                    break;
                case UPDATED:
                    updateCount++;
                    break;
            }
        }

        VBox changesVBox = new VBox(4);
        Label refreshSummary = new Label(TextFactory.format(Messages.MessageKey.REFRESH_STATUS_MESSAGE, addCount, deleteCount, updateCount));
        refreshSummary.setWrapText(true);

        TableView<ComparisonResult> resultTable = new TableView<>();

        final List<String> rowStyleClasses = Arrays.asList(ADDED_ROW, DELETED_ROW, UPDATED_ROW);
        resultTable.setRowFactory(new Callback<TableView<ComparisonResult>, TableRow<ComparisonResult>>() {
            @Override
            public TableRow<ComparisonResult> call(
                TableView<ComparisonResult> param) {

                return new TableRow<ComparisonResult>() {
                    @Override
                    protected void updateItem(ComparisonResult result, boolean empty){
                        super.updateItem(result, empty);
                        if (result != null) {
                            getStyleClass().removeAll(rowStyleClasses);
                            switch (result.getComparison()) {
                                case "ADDED":
                                    getStyleClass().add(ADDED_ROW);
                                    break;
                                case "DELETED":
                                    getStyleClass().add(DELETED_ROW);
                                    break;
                                case "UPDATED":
                                    getStyleClass().add(UPDATED_ROW);
                                    break;
                            }
                        }
                    }
                };
            }
        });
        resultTable.setEditable(false);

        TableColumn<ComparisonResult, String> statusCol = new TableColumn<>(TextFactory.getText(LabelKey.REFRESH_STATUS_LABEL));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("comparison"));
        statusCol.setPrefWidth(75);
        statusCol.setResizable(false);

        TableColumn<ComparisonResult, String> locationCol = new TableColumn<>(TextFactory.getText(LabelKey.REFRESH_LOCATION_LABEL));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));

        resultTable.setItems(resultTableData);
        resultTable.getColumns().add(statusCol);
        resultTable.getColumns().add(locationCol);

        changesVBox.getChildren().add(refreshSummary);
        changesVBox.getChildren().add(resultTable);

        content.getChildren().add(changesVBox);

        HBox controls = new HBox(16);
        controls.setAlignment(Pos.BOTTOM_RIGHT);
        controls.getChildren().add(refreshPopupNegativeButton);
        controls.getChildren().add(refreshPopupPositiveButton);
        content.getChildren().add(controls);

        refreshPopup.setContent(content);

        //Quickly display the popup so we can measure the content
        double x = getScene().getWindow().getX() + getScene().getWidth()/2.0 - 150;
        double y = getScene().getWindow().getY() + getScene().getHeight()/2.0 - 150;
        refreshPopup.setOwner(getScene().getWindow());
        refreshPopup.show(x, y);
        refreshPopup.hide();

        //Get the content width and height to property center the popup.
        x = getScene().getWindow().getX() + getScene().getWidth()/2.0 - content.getWidth()/2.0;
        y = getScene().getWindow().getY() + getScene().getHeight()/2.0 - content.getHeight()/2.0;
        refreshPopup.setOwner(getScene().getWindow());
        refreshPopup.show(x, y);
    }

    public static class ComparisonResult {
        private SimpleStringProperty comparison;
        private SimpleStringProperty location;

        public ComparisonResult(NodeComparison comparison, Node node) {
            this.comparison = new SimpleStringProperty(comparison.getStatus().toString());
            this.location = new SimpleStringProperty(node.getFileInfo().getLocation().toString());
        }

        public String getComparison() {
            return comparison.get();
        }

        public String getLocation() {
            return location.get();
        }
    }

    @Override
    public Map<Node, NodeComparison> getRefreshResult() {
        return refreshResult;
    }

    @Override
    public Button getWarningPopupNegativeButton() {
        return warningPopupNegativeButton;
    }

    @Override
    public Button getRefreshPopupPositiveButton() {
        return refreshPopupPositiveButton;
    }

    @Override
    public Button getRefreshPopupNegativeButton() {
        return refreshPopupNegativeButton;
    }

    @Override
    public CheckBox getHideFutureWarningPopupCheckbox() {
        return hideFutureWarningPopupCheckBox;
    }

    @Override
    public CheckBox getShowIgnored() {
        return showIgnored;
    }

    @Override
    public Map<org.dataconservancy.packaging.tool.model.dprofile.PropertyType, CheckBox> getInheritMetadataCheckBoxMap() {
        return metadataInheritanceButtonMap;
    }
   
    @Override
    public void setupWindowBuilder(){
        String disciplineFilePath = Configuration.resolveConfigurationFile(Configuration.ConfigFile.DISCIPLINE_MAP);
        DisciplineLoadingService disciplineService = new DisciplineLoadingService(disciplineFilePath);
        windowBuilder = new NodePropertyWindowBuilder(cancelPopupLink, applyPopupButton, disciplineService);
    }

    @Override
    public PackageToolPopup getRefreshPopup() {
        return refreshPopup;
    }

    @Override
    public Set<URI> getMissingFileNodes() {
        return nodesMissingFiles;
    }

}
