/*
 * Copyright 2000-2014 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.client.ui.grid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.Touch;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.touch.client.Point;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.HasVisibility;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import com.vaadin.client.DeferredWorker;
import com.vaadin.client.Util;
import com.vaadin.client.data.DataChangeHandler;
import com.vaadin.client.data.DataSource;
import com.vaadin.client.ui.SubPartAware;
import com.vaadin.client.ui.grid.EditorRow.State;
import com.vaadin.client.ui.grid.GridFooter.FooterRow;
import com.vaadin.client.ui.grid.GridHeader.HeaderRow;
import com.vaadin.client.ui.grid.GridStaticSection.StaticCell;
import com.vaadin.client.ui.grid.events.AbstractGridKeyEventHandler;
import com.vaadin.client.ui.grid.events.BodyKeyDownHandler;
import com.vaadin.client.ui.grid.events.BodyKeyPressHandler;
import com.vaadin.client.ui.grid.events.BodyKeyUpHandler;
import com.vaadin.client.ui.grid.events.FooterKeyDownHandler;
import com.vaadin.client.ui.grid.events.FooterKeyPressHandler;
import com.vaadin.client.ui.grid.events.FooterKeyUpHandler;
import com.vaadin.client.ui.grid.events.GridKeyDownEvent;
import com.vaadin.client.ui.grid.events.GridKeyPressEvent;
import com.vaadin.client.ui.grid.events.GridKeyUpEvent;
import com.vaadin.client.ui.grid.events.HeaderKeyDownHandler;
import com.vaadin.client.ui.grid.events.HeaderKeyPressHandler;
import com.vaadin.client.ui.grid.events.HeaderKeyUpHandler;
import com.vaadin.client.ui.grid.events.ScrollEvent;
import com.vaadin.client.ui.grid.events.ScrollHandler;
import com.vaadin.client.ui.grid.renderers.ComplexRenderer;
import com.vaadin.client.ui.grid.renderers.WidgetRenderer;
import com.vaadin.client.ui.grid.selection.HasSelectionChangeHandlers;
import com.vaadin.client.ui.grid.selection.SelectionChangeEvent;
import com.vaadin.client.ui.grid.selection.SelectionChangeHandler;
import com.vaadin.client.ui.grid.selection.SelectionModel;
import com.vaadin.client.ui.grid.selection.SelectionModelMulti;
import com.vaadin.client.ui.grid.selection.SelectionModelNone;
import com.vaadin.client.ui.grid.selection.SelectionModelSingle;
import com.vaadin.client.ui.grid.sort.Sort;
import com.vaadin.client.ui.grid.sort.SortEvent;
import com.vaadin.client.ui.grid.sort.SortEventHandler;
import com.vaadin.client.ui.grid.sort.SortOrder;
import com.vaadin.shared.ui.grid.GridConstants;
import com.vaadin.shared.ui.grid.GridStaticCellType;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.shared.ui.grid.Range;
import com.vaadin.shared.ui.grid.ScrollDestination;
import com.vaadin.shared.ui.grid.SortDirection;
import com.vaadin.shared.ui.grid.SortEventOriginator;
import com.vaadin.shared.util.SharedUtil;

/**
 * A data grid view that supports columns and lazy loading of data rows from a
 * data source.
 * 
 * <h3>Columns</h3>
 * <p>
 * The {@link GridColumn} class defines the renderer used to render a cell in
 * the grid. Implement {@link GridColumn#getValue(Object)} to retrieve the cell
 * value from the row object and return the cell renderer to render that cell.
 * </p>
 * <p>
 * {@link GridColumn}s contain other properties like the width of the column and
 * the visiblity of the column. If you want to change a column's properties
 * after it has been added to the grid you can get a column object for a
 * specific column index using {@link Grid#getColumn(int)}.
 * </p>
 * <p>
 * 
 * TODO Explain about headers/footers once the multiple header/footer api has
 * been implemented
 * 
 * <h3>Data sources</h3>
 * <p>
 * TODO Explain about what a data source is and how it should be implemented.
 * </p>
 * 
 * @param <T>
 *            The row type of the grid. The row type is the POJO type from where
 *            the data is retrieved into the column cells.
 * @since
 * @author Vaadin Ltd
 */
public class Grid<T> extends ResizeComposite implements
        HasSelectionChangeHandlers<T>, SubPartAware, DeferredWorker {

    /**
     * Callback interface for generating custom style names for data rows and
     * cells.
     * 
     * @see Grid#setCellStyleGenerator(CellStyleGenerator)
     */
    public interface CellStyleGenerator<T> {

        /**
         * Called by Grid to generate a style name for a row or cell element.
         * Row styles are generated when the column parameter is
         * <code>null</code>, otherwise a cell style is generated.
         * <p>
         * The returned style name is prefixed so that the actual style for
         * cells will be <tt>v-grid-cell-content-[style name]</tt>, and the row
         * style will be <tt>v-grid-row-[style name]</tt>.
         * 
         * @param grid
         *            the source grid
         * @param row
         *            the data object of the target row
         * @param rowIndex
         *            the index of the row
         * @param column
         *            the column of the cell, <code>null</code> when getting a
         *            row style
         * @param columnIndex
         *            the index of the column, -1 when getting a row style
         * @return the style name to add to this cell or row element, or
         *         <code>null</code> to not set any style
         */
        public abstract String getStyle(Grid<T> grid, T row, int rowIndex,
                GridColumn<?, T> column, int columnIndex);
    }

    public static abstract class AbstractGridKeyEvent<HANDLER extends AbstractGridKeyEventHandler>
            extends KeyEvent<HANDLER> {

        /**
         * Enum describing different section of Grid.
         */
        public enum GridSection {
            HEADER, BODY, FOOTER
        }

        private Grid<?> grid;
        protected Cell focusedCell;
        private final Type<HANDLER> associatedType = new Type<HANDLER>(
                getBrowserEventType(), this);

        public AbstractGridKeyEvent(Grid<?> grid) {
            this.grid = grid;
        }

        protected abstract String getBrowserEventType();

        /**
         * Gets the Grid instance for this event.
         * 
         * @return grid
         */
        public Grid<?> getGrid() {
            return grid;
        }

        /**
         * Gets the focused cell for this event.
         * 
         * @return focused cell
         */
        public Cell getFocusedCell() {
            return focusedCell;
        }

        @Override
        protected void dispatch(HANDLER handler) {
            EventTarget target = getNativeEvent().getEventTarget();
            if (Element.is(target)
                    && !grid.isElementInChildWidget(Element.as(target))) {

                focusedCell = grid.cellFocusHandler.getFocusedCell();
                GridSection section = GridSection.FOOTER;
                final RowContainer container = grid.cellFocusHandler.containerWithFocus;
                if (container == grid.escalator.getHeader()) {
                    section = GridSection.HEADER;
                } else if (container == grid.escalator.getBody()) {
                    section = GridSection.BODY;
                }

                doDispatch(handler, section);
            }
        }

        protected abstract void doDispatch(HANDLER handler, GridSection seciton);

        @Override
        public Type<HANDLER> getAssociatedType() {
            return associatedType;
        }
    }

    private static final String CUSTOM_STYLE_PROPERTY_NAME = "customStyle";

    private GridKeyDownEvent keyDown = new GridKeyDownEvent(this);
    private GridKeyUpEvent keyUp = new GridKeyUpEvent(this);
    private GridKeyPressEvent keyPress = new GridKeyPressEvent(this);

    private class CellFocusHandler {

        private RowContainer containerWithFocus = escalator.getBody();
        private int rowWithFocus = 0;
        private Range cellFocusRange = Range.withLength(0, 1);
        private int lastFocusedBodyRow = 0;
        private int lastFocusedHeaderRow = 0;
        private int lastFocusedFooterRow = 0;
        private TableCellElement cellWithFocusStyle = null;
        private TableRowElement rowWithFocusStyle = null;

        public CellFocusHandler() {
            sinkEvents(getNavigationEvents());
        }

        private Cell getFocusedCell() {
            return new Cell(rowWithFocus, cellFocusRange.getStart(),
                    cellWithFocusStyle);
        }

        /**
         * Sets style names for given cell when needed.
         */
        public void updateFocusedCellStyle(FlyweightCell cell,
                RowContainer cellContainer) {
            int cellRow = cell.getRow();
            int cellColumn = cell.getColumn();
            int colSpan = cell.getColSpan();
            boolean columnHasFocus = Range.withLength(cellColumn, colSpan)
                    .intersects(cellFocusRange);

            if (cellContainer == containerWithFocus) {
                // Cell is in the current container
                if (cellRow == rowWithFocus && columnHasFocus) {
                    if (cellWithFocusStyle != cell.getElement()) {
                        // Cell is correct but it does not have focused style
                        if (cellWithFocusStyle != null) {
                            // Remove old focus style
                            setStyleName(cellWithFocusStyle,
                                    cellFocusStyleName, false);
                        }
                        cellWithFocusStyle = cell.getElement();

                        // Add focus style to correct cell.
                        setStyleName(cellWithFocusStyle, cellFocusStyleName,
                                true);
                    }
                } else if (cellWithFocusStyle == cell.getElement()) {
                    // Due to escalator reusing cells, a new cell has the same
                    // element but is not the focused cell.
                    setStyleName(cellWithFocusStyle, cellFocusStyleName, false);
                    cellWithFocusStyle = null;
                }
            }

            if (cellContainer == escalator.getHeader()
                    || cellContainer == escalator.getFooter()) {
                // Correct header and footer column also needs highlighting
                setStyleName(cell.getElement(), headerFooterFocusStyleName,
                        columnHasFocus);
            }
        }

        /**
         * Sets focus style for the given row if needed.
         * 
         * @param row
         *            a row object
         */
        public void updateFocusedRowStyle(Row row) {
            if (rowWithFocus == row.getRow()
                    && containerWithFocus == escalator.getBody()) {
                if (row.getElement() != rowWithFocusStyle) {
                    // Row should have focus style but does not have it.
                    if (rowWithFocusStyle != null) {
                        setStyleName(rowWithFocusStyle, rowFocusStyleName,
                                false);
                    }
                    rowWithFocusStyle = row.getElement();
                    setStyleName(rowWithFocusStyle, rowFocusStyleName, true);
                }
            } else if (rowWithFocusStyle == row.getElement()
                    || (containerWithFocus != escalator.getBody() && rowWithFocusStyle != null)) {
                // Remove focus style.
                setStyleName(rowWithFocusStyle, rowFocusStyleName, false);
                rowWithFocusStyle = null;
            }
        }

        /**
         * Sets the currently focused.
         * 
         * @param row
         *            the index of the row having focus
         * @param column
         *            the index of the column having focus
         * @param container
         *            the row container having focus
         */
        private void setCellFocus(int row, int column, RowContainer container) {
            if (row == rowWithFocus && cellFocusRange.contains(column)
                    && container == this.containerWithFocus) {
                refreshRow(rowWithFocus);
                return;
            }

            int oldRow = rowWithFocus;
            rowWithFocus = row;
            Range oldRange = cellFocusRange;

            if (container == escalator.getBody()) {
                scrollToRow(rowWithFocus);
                cellFocusRange = Range.withLength(column, 1);
            } else {
                int i = 0;
                Element cell = container.getRowElement(rowWithFocus)
                        .getFirstChildElement();
                do {
                    int colSpan = cell
                            .getPropertyInt(FlyweightCell.COLSPAN_ATTR);
                    Range cellRange = Range.withLength(i, colSpan);
                    if (cellRange.contains(column)) {
                        cellFocusRange = cellRange;
                        break;
                    }
                    cell = cell.getNextSiblingElement();
                    ++i;
                } while (cell != null);
            }

            if (column >= escalator.getColumnConfiguration()
                    .getFrozenColumnCount()) {
                escalator.scrollToColumn(column, ScrollDestination.ANY, 10);
            }

            if (this.containerWithFocus == container) {
                if (oldRange.equals(cellFocusRange) && oldRow != rowWithFocus) {
                    refreshRow(oldRow);
                } else {
                    refreshHeader();
                    refreshFooter();
                }
            } else {
                RowContainer oldContainer = this.containerWithFocus;
                this.containerWithFocus = container;

                if (oldContainer == escalator.getBody()) {
                    lastFocusedBodyRow = oldRow;
                } else if (oldContainer == escalator.getHeader()) {
                    lastFocusedHeaderRow = oldRow;
                } else {
                    lastFocusedFooterRow = oldRow;
                }

                if (!oldRange.equals(cellFocusRange)) {
                    refreshHeader();
                    refreshFooter();
                    if (oldContainer == escalator.getBody()) {
                        oldContainer.refreshRows(oldRow, 1);
                    }
                } else {
                    oldContainer.refreshRows(oldRow, 1);
                }
            }
            refreshRow(rowWithFocus);
        }

        /**
         * Sets focus on a cell.
         * 
         * <p>
         * <em>Note</em>: cell focus is not the same as JavaScript's
         * {@code document.activeElement}.
         * 
         * @param cell
         *            a cell object
         */
        public void setCellFocus(Cell cell) {
            setCellFocus(cell.getRow(), cell.getColumn(),
                    escalator.findRowContainer(cell.getElement()));
        }

        /**
         * Gets list of events that can be used for cell focusing.
         * 
         * @return list of navigation related event types
         */
        public Collection<String> getNavigationEvents() {
            return Arrays.asList(BrowserEvents.KEYDOWN, BrowserEvents.CLICK);
        }

        /**
         * Handle events that can move the cell focus.
         */
        public void handleNavigationEvent(Event event, Cell cell) {
            if (event.getType().equals(BrowserEvents.CLICK)) {
                setCellFocus(cell);
                // Grid should have focus when clicked.
                getElement().focus();
            } else if (event.getType().equals(BrowserEvents.KEYDOWN)) {
                int newRow = rowWithFocus;
                RowContainer newContainer = containerWithFocus;
                int newColumn = cellFocusRange.getStart();

                switch (event.getKeyCode()) {
                case KeyCodes.KEY_DOWN:
                    ++newRow;
                    break;
                case KeyCodes.KEY_UP:
                    --newRow;
                    break;
                case KeyCodes.KEY_RIGHT:
                    if (cellFocusRange.getEnd() >= getVisibleColumns().size()) {
                        return;
                    }
                    newColumn = cellFocusRange.getEnd();
                    break;
                case KeyCodes.KEY_LEFT:
                    if (newColumn == 0) {
                        return;
                    }
                    --newColumn;
                    break;
                case KeyCodes.KEY_TAB:
                    if (event.getShiftKey()) {
                        newContainer = getPreviousContainer(containerWithFocus);
                    } else {
                        newContainer = getNextContainer(containerWithFocus);
                    }

                    if (newContainer == containerWithFocus) {
                        return;
                    }
                    break;
                default:
                    return;
                }

                if (newContainer != containerWithFocus) {
                    if (newContainer == escalator.getBody()) {
                        newRow = lastFocusedBodyRow;
                    } else if (newContainer == escalator.getHeader()) {
                        newRow = lastFocusedHeaderRow;
                    } else {
                        newRow = lastFocusedFooterRow;
                    }
                } else if (newRow < 0) {
                    newContainer = getPreviousContainer(newContainer);

                    if (newContainer == containerWithFocus) {
                        newRow = 0;
                    } else if (newContainer == escalator.getBody()) {
                        newRow = getLastVisibleRowIndex();
                    } else {
                        newRow = newContainer.getRowCount() - 1;
                    }
                } else if (newRow >= containerWithFocus.getRowCount()) {
                    newContainer = getNextContainer(newContainer);

                    if (newContainer == containerWithFocus) {
                        newRow = containerWithFocus.getRowCount() - 1;
                    } else if (newContainer == escalator.getBody()) {
                        newRow = getFirstVisibleRowIndex();
                    } else {
                        newRow = 0;
                    }
                }

                if (newContainer.getRowCount() == 0) {
                    /*
                     * There are no rows in the container. Can't change the
                     * focused cell.
                     */
                    return;
                }

                event.preventDefault();
                event.stopPropagation();

                setCellFocus(newRow, newColumn, newContainer);
            }

        }

        private RowContainer getPreviousContainer(RowContainer current) {
            if (current == escalator.getFooter()) {
                current = escalator.getBody();
            } else if (current == escalator.getBody()) {
                current = escalator.getHeader();
            } else {
                return current;
            }

            if (current.getRowCount() == 0) {
                return getPreviousContainer(current);
            }
            return current;
        }

        private RowContainer getNextContainer(RowContainer current) {
            if (current == escalator.getHeader()) {
                current = escalator.getBody();
            } else if (current == escalator.getBody()) {
                current = escalator.getFooter();
            } else {
                return current;
            }

            if (current.getRowCount() == 0) {
                return getNextContainer(current);
            }
            return current;
        }

        private void refreshRow(int row) {
            containerWithFocus.refreshRows(row, 1);
        }

        /**
         * Offsets the focused cell's range.
         * 
         * @param offset
         *            offset for fixing focused cell's range
         */
        public void offsetRangeBy(int offset) {
            cellFocusRange = cellFocusRange.offsetBy(offset);
        }

        /**
         * Informs {@link CellFocusHandler} that certain range of rows has been
         * added to the Grid body. {@link CellFocusHandler} will fix indices
         * accordingly.
         * 
         * @param added
         *            a range of added rows
         */
        public void rowsAddedToBody(Range added) {
            boolean bodyHasFocus = (containerWithFocus == escalator.getBody());
            boolean insertionIsAboveFocusedCell = (added.getStart() <= rowWithFocus);
            if (bodyHasFocus && insertionIsAboveFocusedCell) {
                setCellFocus(rowWithFocus + added.length(),
                        cellFocusRange.getStart(), containerWithFocus);
            }
        }

        /**
         * Informs {@link CellFocusHandler} that certain range of rows has been
         * removed from the Grid body. {@link CellFocusHandler} will fix indices
         * accordingly.
         * 
         * @param removed
         *            a range of removed rows
         */
        public void rowsRemovedFromBody(Range removed) {
            int focusedColumn = cellFocusRange.getStart();
            if (containerWithFocus != escalator.getBody()) {
                return;
            } else if (!removed.contains(rowWithFocus)) {
                if (removed.getStart() > rowWithFocus) {
                    return;
                }
                setCellFocus(rowWithFocus - removed.length(), focusedColumn,
                        containerWithFocus);
            } else {
                if (containerWithFocus.getRowCount() > removed.getEnd()) {
                    setCellFocus(removed.getStart(), focusedColumn,
                            containerWithFocus);
                } else if (removed.getStart() > 0) {
                    setCellFocus(removed.getStart() - 1, focusedColumn,
                            containerWithFocus);
                } else {
                    if (escalator.getHeader().getRowCount() > 0) {
                        setCellFocus(lastFocusedHeaderRow, focusedColumn,
                                escalator.getHeader());
                    } else if (escalator.getFooter().getRowCount() > 0) {
                        setCellFocus(lastFocusedFooterRow, focusedColumn,
                                escalator.getFooter());
                    }
                }
            }
        }
    }

    public final class SelectionColumn extends GridColumn<Boolean, T> {
        private boolean initDone = false;

        SelectionColumn(final Renderer<Boolean> selectColumnRenderer) {
            super(selectColumnRenderer);
        }

        void initDone() {
            initDone = true;
        }

        @Override
        public void setVisible(boolean visible) {
            if (!visible && initDone) {
                throw new UnsupportedOperationException("The selection "
                        + "column cannot be modified after init");
            } else {
                super.setVisible(visible);
            }
        }

        @Override
        public void setWidth(int pixels) {
            if (pixels != getWidth() && initDone) {
                throw new UnsupportedOperationException("The selection "
                        + "column cannot be modified after init");
            } else {
                super.setWidth(pixels);
            }
        }

        @Override
        public Boolean getValue(T row) {
            return Boolean.valueOf(isSelected(row));
        }
    }

    /**
     * Helper class for performing sorting through the user interface. Controls
     * the sort() method, reporting USER as the event originator. This is a
     * completely internal class, and is, as such, safe to re-name should a more
     * descriptive name come to mind.
     */
    private final class UserSorter {

        private final Timer timer;
        private Cell scheduledCell;
        private boolean scheduledMultisort;

        private UserSorter() {
            timer = new Timer() {
                @Override
                public void run() {
                    UserSorter.this.sort(scheduledCell, scheduledMultisort);
                }
            };
        }

        /**
         * Toggle sorting for a cell. If the multisort parameter is set to true,
         * the cell's sort order is modified as a natural part of a multi-sort
         * chain. If false, the sorting order is set to ASCENDING for that
         * cell's column. If that column was already the only sorted column in
         * the Grid, the sort direction is flipped.
         * 
         * @param cell
         *            a valid cell reference
         * @param multisort
         *            whether the sort command should act as a multi-sort stack
         *            or not
         */
        public void sort(Cell cell, boolean multisort) {

            final GridColumn<?, T> column = getColumnFromVisibleIndex(cell
                    .getColumn());
            if (!column.isSortable()) {
                return;
            }

            final SortOrder so = getSortOrder(column);

            if (multisort) {

                // If the sort order exists, replace existing value with its
                // opposite
                if (so != null) {
                    final int idx = sortOrder.indexOf(so);
                    sortOrder.set(idx, so.getOpposite());
                } else {
                    // If it doesn't, just add a new sort order to the end of
                    // the list
                    sortOrder.add(new SortOrder(column));
                }

            } else {

                // Since we're doing single column sorting, first clear the
                // list. Then, if the sort order existed, add its opposite,
                // otherwise just add a new sort value

                int items = sortOrder.size();
                sortOrder.clear();
                if (so != null && items == 1) {
                    sortOrder.add(so.getOpposite());
                } else {
                    sortOrder.add(new SortOrder(column));
                }
            }

            // sortOrder has been changed; tell the Grid to re-sort itself by
            // user request.
            Grid.this.sort(SortEventOriginator.USER);
        }

        /**
         * Perform a sort after a delay.
         * 
         * @param delay
         *            delay, in milliseconds
         */
        public void sortAfterDelay(int delay, Cell cell, boolean multisort) {
            scheduledCell = cell;
            scheduledMultisort = multisort;
            timer.schedule(delay);
        }

        /**
         * Check if a delayed sort command has been issued but not yet carried
         * out.
         * 
         * @return a boolean value
         */
        public boolean isDelayedSortScheduled() {
            return timer.isRunning();
        }

        /**
         * Cancel a scheduled sort.
         */
        public void cancelDelayedSort() {
            timer.cancel();
        }

    }

    /**
     * Escalator used internally by grid to render the rows
     */
    private Escalator escalator = GWT.create(Escalator.class);

    private final GridHeader header = GWT.create(GridHeader.class);

    private final GridFooter footer = GWT.create(GridFooter.class);

    /**
     * List of columns in the grid. Order defines the visible order.
     */
    private List<GridColumn<?, T>> columns = new ArrayList<GridColumn<?, T>>();

    /**
     * The datasource currently in use. <em>Note:</em> it is <code>null</code>
     * on initialization, but not after that.
     */
    private DataSource<T> dataSource;

    /**
     * Currently available row range in DataSource.
     */
    private Range currentDataAvailable = Range.withLength(0, 0);

    /**
     * The last column frozen counter from the left
     */
    private GridColumn<?, T> lastFrozenColumn;

    /**
     * Current sort order. The (private) sort() method reads this list to
     * determine the order in which to present rows.
     */
    private List<SortOrder> sortOrder = new ArrayList<SortOrder>();

    private Renderer<Boolean> selectColumnRenderer = null;

    private SelectionColumn selectionColumn;

    private String rowStripeStyleName;
    private String rowHasDataStyleName;
    private String rowGeneratedStylePrefix;
    private String rowSelectedStyleName;
    private String cellGeneratedStylePrefix;
    private String cellFocusStyleName;
    private String rowFocusStyleName;
    private String headerFooterFocusStyleName;

    /**
     * Current selection model.
     */
    private SelectionModel<T> selectionModel;

    protected final CellFocusHandler cellFocusHandler;

    private final UserSorter sorter = new UserSorter();

    private final EditorRow<T> editorRow = GWT.create(EditorRow.class);

    private boolean dataIsBeingFetched = false;

    /**
     * The cell a click event originated from
     * <p>
     * This is a workaround to make Chrome work like Firefox. In Chrome,
     * normally if you start a drag on one cell and release on:
     * <ul>
     * <li>that same cell, the click event is that {@code <td>}.
     * <li>a cell on that same row, the click event is the parent {@code <tr>}.
     * <li>a cell on another row, the click event is the table section ancestor
     * ({@code <thead>}, {@code <tbody>} or {@code <tfoot>}).
     * </ul>
     * 
     * @see #onBrowserEvent(Event)
     */
    private Cell cellOnPrevMouseDown;

    /**
     * Enumeration for easy setting of selection mode.
     */
    public enum SelectionMode {

        /**
         * Shortcut for {@link SelectionModelSingle}.
         */
        SINGLE {

            @Override
            protected <T> SelectionModel<T> createModel() {
                return new SelectionModelSingle<T>();
            }
        },

        /**
         * Shortcut for {@link SelectionModelMulti}.
         */
        MULTI {

            @Override
            protected <T> SelectionModel<T> createModel() {
                return new SelectionModelMulti<T>();
            }
        },

        /**
         * Shortcut for {@link SelectionModelNone}.
         */
        NONE {

            @Override
            protected <T> SelectionModel<T> createModel() {
                return new SelectionModelNone<T>();
            }
        };

        protected abstract <T> SelectionModel<T> createModel();
    }

    /**
     * Base class for grid columns internally used by the Grid. The user should
     * use {@link GridColumn} when creating new columns.
     * 
     * @param <C>
     *            the column type
     * 
     * @param <T>
     *            the row type
     */
    static abstract class AbstractGridColumn<C, T> implements HasVisibility {

        /**
         * Default renderer for GridColumns. Renders everything into text
         * through {@link Object#toString()}.
         */
        private final class DefaultTextRenderer implements Renderer<Object> {
            boolean warned = false;
            private final String DEFAULT_RENDERER_WARNING = "This column uses a dummy default TextRenderer. "
                    + "A more suitable renderer should be set using the setRenderer() method.";

            @Override
            public void render(FlyweightCell cell, Object data) {
                if (!warned) {
                    getLogger().warning(
                            AbstractGridColumn.this.toString() + ": "
                                    + DEFAULT_RENDERER_WARNING);
                    warned = true;
                }
                cell.getElement().setInnerText(data.toString());
            }
        }

        /**
         * the column is associated with
         */
        private Grid<T> grid;

        /**
         * Should the column be visible in the grid
         */
        private boolean visible = true;

        /**
         * Width of column in pixels
         */
        private int width = 100;

        /**
         * Renderer for rendering a value into the cell
         */
        private Renderer<? super C> bodyRenderer;

        private boolean sortable = false;

        private String headerText = "";

        /**
         * Constructs a new column with a simple TextRenderer.
         */
        public AbstractGridColumn() {
            setRenderer(new DefaultTextRenderer());
        }

        /**
         * Constructs a new column with a simple TextRenderer.
         * 
         * @param headerText
         *            The header text for this column
         * 
         * @throws IllegalArgumentException
         *             if given header text is null
         */
        public AbstractGridColumn(String headerText)
                throws IllegalArgumentException {
            this();
            setHeaderText(headerText);
        }

        /**
         * Constructs a new column with a custom renderer.
         * 
         * @param renderer
         *            The renderer to use for rendering the cells
         * 
         * @throws IllegalArgumentException
         *             if given Renderer is null
         */
        public AbstractGridColumn(Renderer<? super C> renderer)
                throws IllegalArgumentException {
            setRenderer(renderer);
        }

        /**
         * Constructs a new column with a custom renderer.
         * 
         * @param renderer
         *            The renderer to use for rendering the cells
         * @param headerText
         *            The header text for this column
         * 
         * @throws IllegalArgumentException
         *             if given Renderer or header text is null
         */
        public AbstractGridColumn(String headerText,
                Renderer<? super C> renderer) throws IllegalArgumentException {
            this(renderer);
            setHeaderText(headerText);
        }

        /**
         * Internally used by the grid to set itself
         * 
         * @param grid
         */
        private void setGrid(Grid<T> grid) {
            if (this.grid != null && grid != null) {
                // Trying to replace grid
                throw new IllegalStateException("Column already is attached "
                        + "to a grid. Remove the column first from the grid "
                        + "and then add it. (in: " + toString() + ")");
            }

            this.grid = grid;
            if (grid != null) {
                updateHeader();
            }
        }

        /**
         * Sets a header text for this column.
         * 
         * @param headerText
         *            The header text for this column
         * 
         * @throws IllegalArgumentException
         *             if given header text is null
         */
        public void setHeaderText(String headerText) {
            if (headerText == null) {
                throw new IllegalArgumentException(
                        "Header text cannot be null.");
            }

            if (!this.headerText.equals(headerText)) {
                this.headerText = headerText;
                if (grid != null) {
                    updateHeader();
                }
            }
        }

        private void updateHeader() {
            HeaderRow row = grid.getHeader().getDefaultRow();
            if (row != null) {
                row.getCell((GridColumn<?, ?>) this).setText(headerText);
            }
        }

        /**
         * Is the column visible. By default all columns are visible.
         * 
         * @return <code>true</code> if the column is visible
         */
        @Override
        public boolean isVisible() {
            return visible;
        }

        /**
         * Sets a column as visible in the grid.
         * 
         * @param visible
         *            <code>true</code> if the column should be displayed in the
         *            grid
         */
        @Override
        public void setVisible(boolean visible) {
            if (this.visible == visible) {
                return;
            }

            /*
             * We need to guarantee that both insertColumns and removeColumns
             * have this particular column accessible. Therefore, if we're
             * turning the column visible, it's set before the other logic.
             * Analogously, if we're turning the column invisible, we do that
             * only after the logic has been performed.
             */

            if (visible) {
                this.visible = true;
            }

            if (grid != null) {
                int index = findIndexOfColumn();
                ColumnConfiguration conf = grid.escalator
                        .getColumnConfiguration();

                if (visible) {
                    conf.insertColumns(index, 1);
                } else {
                    conf.removeColumns(index, 1);
                }
            }

            if (!visible) {
                this.visible = false;
            }

            if (grid != null) {
                for (HeaderRow row : grid.getHeader().getRows()) {
                    row.calculateColspans();
                }

                for (FooterRow row : grid.getFooter().getRows()) {
                    row.calculateColspans();
                }
            }
        }

        /**
         * Returns the data that should be rendered into the cell. By default
         * returning Strings and Widgets are supported. If the return type is a
         * String then it will be treated as preformatted text.
         * <p>
         * To support other types you will need to pass a custom renderer to the
         * column via the column constructor.
         * 
         * @param row
         *            The row object that provides the cell content.
         * 
         * @return The cell content
         */
        public abstract C getValue(T row);

        /**
         * The renderer to render the cell width. By default renders the data as
         * a String or adds the widget into the cell if the column type is of
         * widget type.
         * 
         * @return The renderer to render the cell content with
         */
        public Renderer<? super C> getRenderer() {
            return bodyRenderer;
        }

        /**
         * Sets a custom {@link Renderer} for this column.
         * 
         * @param renderer
         *            The renderer to use for rendering the cells
         * 
         * @throws IllegalArgumentException
         *             if given Renderer is null
         */
        public void setRenderer(Renderer<? super C> renderer)
                throws IllegalArgumentException {
            if (renderer == null) {
                throw new IllegalArgumentException("Renderer cannot be null.");
            }
            bodyRenderer = renderer;

            if (grid != null) {
                grid.refreshBody();
            }
        }

        /**
         * Finds the index of this column instance
         * 
         */
        private int findIndexOfColumn() {
            return grid.findVisibleColumnIndex((GridColumn<?, T>) this);
        }

        /**
         * Sets the pixel width of the column. Use a negative value for the grid
         * to autosize column based on content and available space
         * 
         * @param pixels
         *            the width in pixels or negative for auto sizing
         */
        public void setWidth(int pixels) {
            width = pixels;

            if (grid != null && isVisible()) {
                int index = findIndexOfColumn();
                ColumnConfiguration conf = grid.escalator
                        .getColumnConfiguration();
                conf.setColumnWidth(index, pixels);
            }
        }

        /**
         * Returns the pixel width of the column
         * 
         * @return pixel width of the column
         */
        public int getWidth() {
            return width;
        }

        void reapplyWidth() {
            setWidth(width);
        }

        /**
         * Enables sort indicators for the grid.
         * <p>
         * <b>Note:</b>The API can still sort the column even if this is set to
         * <code>false</code>.
         * 
         * @param sortable
         *            <code>true</code> when column sort indicators are visible.
         */
        public void setSortable(boolean sortable) {
            if (this.sortable != sortable) {
                this.sortable = sortable;
                if (grid != null) {
                    grid.refreshHeader();
                }
            }
        }

        /**
         * Are sort indicators shown for the column.
         * 
         * @return <code>true</code> if the column is sortable
         */
        public boolean isSortable() {
            return sortable;
        }

        @Override
        public String toString() {
            String details = "";

            if (headerText != null && !headerText.isEmpty()) {
                details += "header:\"" + headerText + "\" ";
            } else {
                details += "header:empty ";
            }

            if (grid != null) {
                int index = grid.getColumns().indexOf(this);
                if (index != -1) {
                    details += "attached:#" + index + " ";
                } else {
                    details += "attached:unindexed ";
                }
            } else {
                details += "detached ";
            }

            details += "visible:" + visible + " ";
            details += "sortable:" + sortable + " ";

            return getClass().getSimpleName() + "[" + details.trim() + "]";
        }
    }

    protected class BodyUpdater implements EscalatorUpdater {

        @Override
        public void preAttach(Row row, Iterable<FlyweightCell> cellsToAttach) {
            for (FlyweightCell cell : cellsToAttach) {
                Renderer<?> renderer = findRenderer(cell);
                if (renderer instanceof ComplexRenderer) {
                    ((ComplexRenderer<?>) renderer).init(cell);
                }
            }
        }

        @Override
        public void postAttach(Row row, Iterable<FlyweightCell> attachedCells) {
            for (FlyweightCell cell : attachedCells) {
                Renderer<?> renderer = findRenderer(cell);
                if (renderer instanceof WidgetRenderer) {
                    WidgetRenderer<?, ?> widgetRenderer = (WidgetRenderer<?, ?>) renderer;

                    Widget widget = widgetRenderer.createWidget();
                    assert widget != null : "WidgetRenderer.createWidget() returned null. It should return a widget.";
                    assert widget.getParent() == null : "WidgetRenderer.createWidget() returned a widget which already is attached.";
                    assert cell.getElement().getChildCount() == 0 : "Cell content should be empty when adding Widget";

                    // Physical attach
                    cell.getElement().appendChild(widget.getElement());

                    // Logical attach
                    GridUtil.setParent(widget, Grid.this);
                }
            }
        }

        @Override
        public void update(Row row, Iterable<FlyweightCell> cellsToUpdate) {
            int rowIndex = row.getRow();
            TableRowElement rowElement = row.getElement();
            T rowData = dataSource.getRow(rowIndex);

            boolean hasData = rowData != null;

            /*
             * TODO could be more efficient to build a list of all styles that
             * should be used and update the element only once instead of
             * attempting to update only the ones that have changed.
             */

            // Assign stylename for rows with data
            boolean usedToHaveData = rowElement
                    .hasClassName(rowHasDataStyleName);

            if (usedToHaveData != hasData) {
                setStyleName(rowElement, rowHasDataStyleName, hasData);
            }

            boolean isEvenIndex = (row.getRow() % 2 == 0);
            setStyleName(rowElement, rowStripeStyleName, isEvenIndex);

            if (hasData) {
                setStyleName(rowElement, rowSelectedStyleName,
                        isSelected(rowData));

                if (cellStyleGenerator != null) {
                    String rowStylename = cellStyleGenerator.getStyle(
                            Grid.this, rowData, rowIndex, null, -1);
                    if (rowStylename != null) {
                        rowStylename = rowGeneratedStylePrefix + rowStylename;
                    }
                    setCustomStyleName(rowElement, rowStylename);
                } else {
                    // Remove in case there was a generator previously
                    setCustomStyleName(rowElement, null);
                }
            } else if (usedToHaveData) {
                setStyleName(rowElement, rowSelectedStyleName, false);

                setCustomStyleName(rowElement, null);
            }

            cellFocusHandler.updateFocusedRowStyle(row);

            for (FlyweightCell cell : cellsToUpdate) {
                GridColumn<?, T> column = getColumnFromVisibleIndex(cell
                        .getColumn());

                assert column != null : "Column was not found from cell ("
                        + cell.getColumn() + "," + cell.getRow() + ")";

                cellFocusHandler.updateFocusedCellStyle(cell,
                        escalator.getBody());

                if (hasData && cellStyleGenerator != null) {
                    String generatedStyle = cellStyleGenerator.getStyle(
                            Grid.this, rowData, rowIndex, column,
                            cell.getColumn());
                    if (generatedStyle != null) {
                        generatedStyle = cellGeneratedStylePrefix
                                + generatedStyle;
                    }
                    setCustomStyleName(cell.getElement(), generatedStyle);
                } else if (hasData || usedToHaveData) {
                    setCustomStyleName(cell.getElement(), null);
                }

                Renderer renderer = column.getRenderer();

                if (renderer instanceof ComplexRenderer) {
                    // Hide cell content if needed
                    ComplexRenderer clxRenderer = (ComplexRenderer) renderer;
                    if (hasData) {
                        if (!usedToHaveData) {
                            // Prepare cell for rendering
                            clxRenderer.setContentVisible(cell, true);
                        }

                        Object value = column.getValue(rowData);
                        clxRenderer.render(cell, value);

                    } else {
                        // Prepare cell for no data
                        clxRenderer.setContentVisible(cell, false);
                    }

                } else if (hasData) {
                    // Simple renderers just render
                    Object value = column.getValue(rowData);
                    renderer.render(cell, value);

                } else {
                    // Clear cell if there is no data
                    cell.getElement().removeAllChildren();
                }
            }
        }

        @Override
        public void preDetach(Row row, Iterable<FlyweightCell> cellsToDetach) {
            for (FlyweightCell cell : cellsToDetach) {
                Renderer renderer = findRenderer(cell);
                if (renderer instanceof WidgetRenderer) {
                    Widget w = Util.findWidget(cell.getElement()
                            .getFirstChildElement(), Widget.class);
                    if (w != null) {

                        // Logical detach
                        GridUtil.setParent(w, null);

                        // Physical detach
                        cell.getElement().removeChild(w.getElement());
                    }
                }
            }
        }

        @Override
        public void postDetach(Row row, Iterable<FlyweightCell> detachedCells) {
            for (FlyweightCell cell : detachedCells) {
                Renderer renderer = findRenderer(cell);
                if (renderer instanceof ComplexRenderer) {
                    ((ComplexRenderer) renderer).destroy(cell);
                }
            }
        }
    }

    protected class StaticSectionUpdater implements EscalatorUpdater {

        private GridStaticSection<?> section;
        private RowContainer container;

        public StaticSectionUpdater(GridStaticSection<?> section,
                RowContainer container) {
            super();
            this.section = section;
            this.container = container;
        }

        @Override
        public void update(Row row, Iterable<FlyweightCell> cellsToUpdate) {
            GridStaticSection.StaticRow<?> staticRow = section.getRow(row
                    .getRow());
            final List<GridColumn<?, T>> columns = getVisibleColumns();

            setCustomStyleName(row.getElement(), staticRow.getStyleName());

            for (FlyweightCell cell : cellsToUpdate) {
                final StaticCell metadata = staticRow.getCell(columns.get(cell
                        .getColumn()));

                // Decorate default row with sorting indicators
                if (staticRow instanceof HeaderRow) {
                    addSortingIndicatorsToHeaderRow((HeaderRow) staticRow, cell);
                }

                // Assign colspan to cell before rendering
                cell.setColSpan(metadata.getColspan());

                TableCellElement element = cell.getElement();
                switch (metadata.getType()) {
                case TEXT:
                    element.setInnerText(metadata.getText());
                    break;
                case HTML:
                    element.setInnerHTML(metadata.getHtml());
                    break;
                case WIDGET:
                    preDetach(row, Arrays.asList(cell));
                    element.setInnerHTML("");
                    postAttach(row, Arrays.asList(cell));
                    break;
                }
                setCustomStyleName(element, metadata.getStyleName());

                cellFocusHandler.updateFocusedCellStyle(cell, container);
            }
        }

        private void addSortingIndicatorsToHeaderRow(HeaderRow headerRow,
                FlyweightCell cell) {

            cleanup(cell);

            GridColumn<?, ?> column = getColumnFromVisibleIndex(cell
                    .getColumn());
            SortOrder sortingOrder = getSortOrder(column);
            if (!headerRow.isDefault() || !column.isSortable()
                    || sortingOrder == null) {
                // Only apply sorting indicators to sortable header columns in
                // the default header row
                return;
            }

            Element cellElement = cell.getElement();

            if (SortDirection.ASCENDING == sortingOrder.getDirection()) {
                cellElement.addClassName("sort-asc");
            } else {
                cellElement.addClassName("sort-desc");
            }

            int sortIndex = Grid.this.getSortOrder().indexOf(sortingOrder);
            if (sortIndex > -1 && Grid.this.getSortOrder().size() > 1) {
                // Show sort order indicator if column is
                // sorted and other sorted columns also exists.
                cellElement.setAttribute("sort-order",
                        String.valueOf(sortIndex + 1));
            }
        }

        /**
         * Finds the sort order for this column
         */
        private SortOrder getSortOrder(GridColumn<?, ?> column) {
            for (SortOrder order : Grid.this.getSortOrder()) {
                if (order.getColumn() == column) {
                    return order;
                }
            }
            return null;
        }

        private void cleanup(FlyweightCell cell) {
            Element cellElement = cell.getElement();
            cellElement.removeAttribute("sort-order");
            cellElement.removeClassName("sort-desc");
            cellElement.removeClassName("sort-asc");
        }

        @Override
        public void preAttach(Row row, Iterable<FlyweightCell> cellsToAttach) {
        }

        @Override
        public void postAttach(Row row, Iterable<FlyweightCell> attachedCells) {
            GridStaticSection.StaticRow<?> gridRow = section.getRow(row
                    .getRow());
            List<GridColumn<?, T>> columns = getVisibleColumns();

            for (FlyweightCell cell : attachedCells) {
                StaticCell metadata = gridRow.getCell(columns.get(cell
                        .getColumn()));
                /*
                 * If the cell contains widgets that are not currently attach
                 * then attach them now.
                 */
                if (GridStaticCellType.WIDGET.equals(metadata.getType())) {
                    final Widget widget = metadata.getWidget();
                    final Element cellElement = cell.getElement();

                    if (!widget.isAttached()) {

                        // Physical attach
                        cellElement.appendChild(widget.getElement());

                        // Logical attach
                        GridUtil.setParent(widget, Grid.this);
                    }
                }
            }
        }

        @Override
        public void preDetach(Row row, Iterable<FlyweightCell> cellsToDetach) {
            if (section.getRowCount() > row.getRow()) {
                GridStaticSection.StaticRow<?> gridRow = section.getRow(row
                        .getRow());
                List<GridColumn<?, T>> columns = getVisibleColumns();
                for (FlyweightCell cell : cellsToDetach) {
                    StaticCell metadata = gridRow.getCell(columns.get(cell
                            .getColumn()));

                    if (GridStaticCellType.WIDGET.equals(metadata.getType())
                            && metadata.getWidget().isAttached()) {

                        Widget widget = metadata.getWidget();

                        // Logical detach
                        GridUtil.setParent(widget, null);

                        // Physical detach
                        widget.getElement().removeFromParent();
                    }
                }
            }
        }

        @Override
        public void postDetach(Row row, Iterable<FlyweightCell> detachedCells) {
        }
    };

    /**
     * Creates a new instance.
     */
    public Grid() {
        initWidget(escalator);
        getElement().setTabIndex(0);
        cellFocusHandler = new CellFocusHandler();

        setStylePrimaryName("v-grid");

        escalator.getHeader().setEscalatorUpdater(createHeaderUpdater());
        escalator.getBody().setEscalatorUpdater(createBodyUpdater());
        escalator.getFooter().setEscalatorUpdater(createFooterUpdater());

        header.setGrid(this);
        HeaderRow defaultRow = header.appendRow();
        header.setDefaultRow(defaultRow);

        footer.setGrid(this);

        editorRow.setGrid(this);

        setSelectionMode(SelectionMode.MULTI);

        escalator.addScrollHandler(new ScrollHandler() {
            @Override
            public void onScroll(ScrollEvent event) {
                fireEvent(new ScrollEvent());
            }
        });

        escalator
                .addRowVisibilityChangeHandler(new RowVisibilityChangeHandler() {
                    @Override
                    public void onRowVisibilityChange(
                            RowVisibilityChangeEvent event) {
                        if (dataSource != null && dataSource.size() > 0) {
                            dataIsBeingFetched = true;
                            dataSource.ensureAvailability(
                                    event.getFirstVisibleRow(),
                                    event.getVisibleRowCount());
                        }
                    }
                });

        // Default action on SelectionChangeEvents. Refresh the body so changed
        // become visible.
        addSelectionChangeHandler(new SelectionChangeHandler<T>() {

            @Override
            public void onSelectionChange(SelectionChangeEvent<T> event) {
                refreshBody();
            }
        });

        // Sink header events and key events
        sinkEvents(getHeader().getConsumedEvents());
        sinkEvents(Arrays.asList(BrowserEvents.KEYDOWN, BrowserEvents.KEYUP,
                BrowserEvents.KEYPRESS, BrowserEvents.DBLCLICK));

        // Make ENTER and SHIFT+ENTER in the header perform sorting
        addHeaderKeyUpHandler(new HeaderKeyUpHandler() {
            @Override
            public void onKeyUp(GridKeyUpEvent event) {
                if (event.getNativeKeyCode() != KeyCodes.KEY_ENTER) {
                    return;
                }

                sorter.sort(event.getFocusedCell(), event.isShiftKeyDown());
            }
        });

        addDataAvailableHandler(new DataAvailableHandler() {
            @Override
            public void onDataAvailable(DataAvailableEvent event) {
                dataIsBeingFetched = false;
            }
        });
    }

    @Override
    public void setStylePrimaryName(String style) {
        super.setStylePrimaryName(style);
        escalator.setStylePrimaryName(style);
        editorRow.setStylePrimaryName(style);

        String rowStyle = getStylePrimaryName() + "-row";
        rowHasDataStyleName = rowStyle + "-has-data";
        rowSelectedStyleName = rowStyle + "-selected";
        rowStripeStyleName = rowStyle + "-stripe";
        rowGeneratedStylePrefix = rowStyle + "-";

        /*
         * TODO rename CSS "active" to "focused" once Valo theme has been
         * merged.
         */
        cellFocusStyleName = getStylePrimaryName() + "-cell-active";
        cellGeneratedStylePrefix = getStylePrimaryName() + "-cell-content-";
        headerFooterFocusStyleName = getStylePrimaryName() + "-header-active";
        rowFocusStyleName = getStylePrimaryName() + "-row-active";

        if (isAttached()) {
            refreshHeader();
            refreshBody();
            refreshFooter();
        }
    }

    /**
     * Creates the escalator updater used to update the header rows in this
     * grid. The updater is invoked when header rows or columns are added or
     * removed, or the content of existing header cells is changed.
     * 
     * @return the new header updater instance
     * 
     * @see GridHeader
     * @see Grid#getHeader()
     */
    protected EscalatorUpdater createHeaderUpdater() {
        return new StaticSectionUpdater(header, escalator.getHeader());
    }

    /**
     * Creates the escalator updater used to update the body rows in this grid.
     * The updater is invoked when body rows or columns are added or removed,
     * the content of body cells is changed, or the body is scrolled to expose
     * previously hidden content.
     * 
     * @return the new body updater instance
     */
    protected EscalatorUpdater createBodyUpdater() {
        return new BodyUpdater();
    }

    /**
     * Creates the escalator updater used to update the footer rows in this
     * grid. The updater is invoked when header rows or columns are added or
     * removed, or the content of existing header cells is changed.
     * 
     * @return the new footer updater instance
     * 
     * @see GridFooter
     * @see #getFooter()
     */
    protected EscalatorUpdater createFooterUpdater() {
        return new StaticSectionUpdater(footer, escalator.getFooter());
    }

    /**
     * Refreshes header or footer rows on demand
     * 
     * @param rows
     *            The row container
     * @param firstRowIsVisible
     *            is the first row visible
     * @param isHeader
     *            <code>true</code> if we refreshing the header, else assumed
     *            the footer
     */
    private void refreshRowContainer(RowContainer rows,
            GridStaticSection<?> section) {

        // Add or Remove rows on demand
        int rowDiff = section.getVisibleRowCount() - rows.getRowCount();
        if (rowDiff > 0) {
            rows.insertRows(0, rowDiff);
        } else if (rowDiff < 0) {
            rows.removeRows(0, -rowDiff);
        }

        // Refresh all the rows
        if (rows.getRowCount() > 0) {
            rows.refreshRows(0, rows.getRowCount());
        }
    }

    /**
     * Refreshes all header rows
     */
    void refreshHeader() {
        refreshRowContainer(escalator.getHeader(), header);
    }

    /**
     * Refreshes all body rows
     */
    private void refreshBody() {
        escalator.getBody().refreshRows(0, escalator.getBody().getRowCount());
    }

    /**
     * Refreshes all footer rows
     */
    void refreshFooter() {
        refreshRowContainer(escalator.getFooter(), footer);
    }

    /**
     * Adds columns as the last columns in the grid.
     * 
     * @param columns
     *            the columns to add
     */
    public void addColumns(GridColumn<?, T>... columns) {
        int count = getColumnCount();
        for (GridColumn<?, T> column : columns) {
            addColumn(column, count++);
        }
    }

    /**
     * Adds a column as the last column in the grid.
     * 
     * @param column
     *            the column to add
     */
    public void addColumn(GridColumn<?, T> column) {
        addColumn(column, getColumnCount());
    }

    /**
     * Inserts a column into a specific position in the grid.
     * 
     * @param index
     *            the index where the column should be inserted into
     * @param column
     *            the column to add
     * @throws IllegalStateException
     *             if Grid's current selection model renders a selection column,
     *             and {@code index} is 0.
     */
    public void addColumn(GridColumn<?, T> column, int index) {
        if (column == selectionColumn) {
            throw new IllegalArgumentException("The selection column many "
                    + "not be added manually");
        } else if (selectionColumn != null && index == 0) {
            throw new IllegalStateException("A column cannot be inserted "
                    + "before the selection column");
        }

        addColumnSkipSelectionColumnCheck(column, index);
    }

    private void addColumnSkipSelectionColumnCheck(GridColumn<?, T> column,
            int index) {
        // Register column with grid
        columns.add(index, column);

        header.addColumn(column);
        footer.addColumn(column);

        // Register this grid instance with the column
        ((AbstractGridColumn<?, T>) column).setGrid(this);

        // Insert column into escalator
        if (column.isVisible()) {
            int visibleIndex = findVisibleColumnIndex(column);
            ColumnConfiguration conf = escalator.getColumnConfiguration();

            // Insert column
            conf.insertColumns(visibleIndex, 1);

            // Transfer column width from column object to escalator
            conf.setColumnWidth(visibleIndex, column.getWidth());
        }

        if (lastFrozenColumn != null
                && ((AbstractGridColumn<?, T>) lastFrozenColumn)
                        .findIndexOfColumn() < index) {
            refreshFrozenColumns();
        }

        // Sink all renderer events
        Set<String> events = new HashSet<String>();
        events.addAll(getConsumedEventsForRenderer(column.getRenderer()));

        sinkEvents(events);
    }

    private void sinkEvents(Collection<String> events) {
        assert events != null;

        int eventsToSink = 0;
        for (String typeName : events) {
            int typeInt = Event.getTypeInt(typeName);
            if (typeInt < 0) {
                // Type not recognized by typeInt
                sinkBitlessEvent(typeName);
            } else {
                eventsToSink |= typeInt;
            }
        }

        if (eventsToSink > 0) {
            sinkEvents(eventsToSink);
        }
    }

    protected int findVisibleColumnIndex(GridColumn<?, T> column) {
        int idx = 0;
        for (GridColumn<?, T> c : columns) {
            if (c == column) {
                return idx;
            } else if (c.isVisible()) {
                idx++;
            }
        }
        return -1;
    }

    protected GridColumn<?, T> getColumnFromVisibleIndex(int index) {
        int idx = -1;
        for (GridColumn<?, T> c : columns) {
            if (c.isVisible()) {
                idx++;
            }
            if (index == idx) {
                return c;
            }
        }
        return null;
    }

    private Renderer<?> findRenderer(FlyweightCell cell) {
        GridColumn<?, T> column = getColumnFromVisibleIndex(cell.getColumn());
        assert column != null : "Could not find column at index:"
                + cell.getColumn();
        return column.getRenderer();
    }

    /**
     * Removes a column from the grid.
     * 
     * @param column
     *            the column to remove
     */
    public void removeColumn(GridColumn<?, T> column) {
        if (column != null && column.equals(selectionColumn)) {
            throw new IllegalArgumentException(
                    "The selection column may not be removed manually.");
        }

        removeColumnSkipSelectionColumnCheck(column);
    }

    private void removeColumnSkipSelectionColumnCheck(GridColumn<?, T> column) {
        int columnIndex = columns.indexOf(column);
        int visibleIndex = findVisibleColumnIndex(column);

        if (column.isVisible()) {
            ColumnConfiguration conf = escalator.getColumnConfiguration();
            conf.removeColumns(visibleIndex, 1);
        }

        if (column.equals(lastFrozenColumn)) {
            setLastFrozenColumn(null);
        } else {
            refreshFrozenColumns();
        }

        header.removeColumn(column);
        footer.removeColumn(column);

        // de-register column with grid
        ((AbstractGridColumn<?, T>) column).setGrid(null);

        columns.remove(columnIndex);
    }

    /**
     * Returns the amount of columns in the grid.
     * 
     * @return The number of columns in the grid
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * Returns a list of columns in the grid.
     * 
     * @return A unmodifiable list of the columns in the grid
     */
    public List<GridColumn<?, T>> getColumns() {
        return Collections.unmodifiableList(new ArrayList<GridColumn<?, T>>(
                columns));
    }

    /**
     * Returns a column by its index in the grid.
     * 
     * @param index
     *            the index of the column
     * @return The column in the given index
     * @throws IllegalArgumentException
     *             if the column index does not exist in the grid
     */
    public GridColumn<?, T> getColumn(int index)
            throws IllegalArgumentException {
        if (index < 0 || index >= columns.size()) {
            throw new IllegalStateException("Column not found.");
        }
        return columns.get(index);
    }

    /**
     * Returns a list of columns that are currently visible.
     * 
     * @return a list of columns
     */
    protected List<GridColumn<?, T>> getVisibleColumns() {
        List<GridColumn<?, T>> visible = new ArrayList<GridColumn<?, T>>();
        for (GridColumn<?, T> column : getColumns()) {
            if (column.isVisible()) {
                visible.add(column);
            }
        }
        return visible;
    }

    /**
     * Returns the header section of this grid. The default header contains a
     * single row displaying the column captions.
     * 
     * @return the header
     */
    public GridHeader getHeader() {
        return header;
    }

    /**
     * Returns the footer section of this grid. The default footer is empty.
     * 
     * @return the footer
     */
    public GridFooter getFooter() {
        return footer;
    }

    public EditorRow<T> getEditorRow() {
        return editorRow;
    }

    protected Escalator getEscalator() {
        return escalator;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <em>Note:</em> This method will change the widget's size in the browser
     * only if {@link #getHeightMode()} returns {@link HeightMode#CSS}.
     * 
     * @see #setHeightMode(HeightMode)
     */
    @Override
    public void setHeight(String height) {
        escalator.setHeight(height);
    }

    @Override
    public void setWidth(String width) {
        escalator.setWidth(width);
    }

    /**
     * Sets the data source used by this grid.
     * 
     * @param dataSource
     *            the data source to use, not null
     * @throws IllegalArgumentException
     *             if <code>dataSource</code> is <code>null</code>
     */
    public void setDataSource(final DataSource<T> dataSource)
            throws IllegalArgumentException {
        if (dataSource == null) {
            throw new IllegalArgumentException("dataSource can't be null.");
        }

        selectionModel.reset();

        if (this.dataSource != null) {
            this.dataSource.setDataChangeHandler(null);
        }

        this.dataSource = dataSource;
        dataSource.setDataChangeHandler(new DataChangeHandler() {
            @Override
            public void dataUpdated(int firstIndex, int numberOfItems) {
                escalator.getBody().refreshRows(firstIndex, numberOfItems);
            }

            @Override
            public void dataRemoved(int firstIndex, int numberOfItems) {
                escalator.getBody().removeRows(firstIndex, numberOfItems);
                Range removed = Range.withLength(firstIndex, numberOfItems);
                cellFocusHandler.rowsRemovedFromBody(removed);
            }

            @Override
            public void dataAdded(int firstIndex, int numberOfItems) {
                escalator.getBody().insertRows(firstIndex, numberOfItems);
                Range added = Range.withLength(firstIndex, numberOfItems);
                cellFocusHandler.rowsAddedToBody(added);
            }

            @Override
            public void dataAvailable(int firstIndex, int numberOfItems) {
                currentDataAvailable = Range.withLength(firstIndex,
                        numberOfItems);
                fireEvent(new DataAvailableEvent(currentDataAvailable));
            }

            @Override
            public void resetDataAndSize(int newSize) {
                RowContainer body = escalator.getBody();
                int oldSize = body.getRowCount();

                if (newSize > oldSize) {
                    body.insertRows(oldSize, newSize - oldSize);
                } else if (newSize < oldSize) {
                    body.removeRows(newSize, oldSize - newSize);
                }

                Range visibleRowRange = escalator.getVisibleRowRange();
                dataSource.ensureAvailability(visibleRowRange.getStart(),
                        visibleRowRange.length());

                assert body.getRowCount() == newSize;
            }
        });

        int previousRowCount = escalator.getBody().getRowCount();
        if (previousRowCount != 0) {
            escalator.getBody().removeRows(0, previousRowCount);
        }

        int size = dataSource.size();
        if (size > 0) {
            escalator.getBody().insertRows(0, size);
        }

    }

    /**
     * Gets the {@Link DataSource} for this Grid.
     * 
     * @return the data source used by this grid
     */
    public DataSource<T> getDataSource() {
        return dataSource;
    }

    /**
     * Sets the rightmost frozen column in the grid.
     * <p>
     * All columns up to and including the given column will be frozen in place
     * when the grid is scrolled sideways.
     * 
     * @param lastFrozenColumn
     *            the rightmost column to freeze, or <code>null</code> to not
     *            have any columns frozen
     * @throws IllegalArgumentException
     *             if {@code lastFrozenColumn} is not a column from this grid
     */
    public void setLastFrozenColumn(GridColumn<?, T> lastFrozenColumn) {
        this.lastFrozenColumn = lastFrozenColumn;
        refreshFrozenColumns();
    }

    private void refreshFrozenColumns() {
        final int frozenCount;
        if (lastFrozenColumn != null) {
            frozenCount = columns.indexOf(lastFrozenColumn) + 1;
            if (frozenCount == 0) {
                throw new IllegalArgumentException(
                        "The given column isn't attached to this grid");
            }
        } else {
            frozenCount = 0;
        }

        escalator.getColumnConfiguration().setFrozenColumnCount(frozenCount);
    }

    /**
     * Gets the rightmost frozen column in the grid.
     * <p>
     * <em>Note:</em> Most usually, this method returns the very value set with
     * {@link #setLastFrozenColumn(GridColumn)}. This value, however, can be
     * reset to <code>null</code> if the column is removed from this grid.
     * 
     * @return the rightmost frozen column in the grid, or <code>null</code> if
     *         no columns are frozen.
     */
    public GridColumn<?, T> getLastFrozenColumn() {
        return lastFrozenColumn;
    }

    public HandlerRegistration addRowVisibilityChangeHandler(
            RowVisibilityChangeHandler handler) {
        /*
         * Reusing Escalator's RowVisibilityChangeHandler, since a scroll
         * concept is too abstract. e.g. the event needs to be re-sent when the
         * widget is resized.
         */
        return escalator.addRowVisibilityChangeHandler(handler);
    }

    /**
     * Scrolls to a certain row, using {@link ScrollDestination#ANY}.
     * 
     * @param rowIndex
     *            zero-based index of the row to scroll to.
     * @throws IllegalArgumentException
     *             if rowIndex is below zero, or above the maximum value
     *             supported by the data source.
     */
    public void scrollToRow(int rowIndex) throws IllegalArgumentException {
        scrollToRow(rowIndex, ScrollDestination.ANY,
                GridConstants.DEFAULT_PADDING);
    }

    /**
     * Scrolls to a certain row, using user-specified scroll destination.
     * 
     * @param rowIndex
     *            zero-based index of the row to scroll to.
     * @param destination
     *            desired destination placement of scrolled-to-row. See
     *            {@link ScrollDestination} for more information.
     * @throws IllegalArgumentException
     *             if rowIndex is below zero, or above the maximum value
     *             supported by the data source.
     */
    public void scrollToRow(int rowIndex, ScrollDestination destination)
            throws IllegalArgumentException {
        scrollToRow(rowIndex, destination,
                destination == ScrollDestination.MIDDLE ? 0
                        : GridConstants.DEFAULT_PADDING);
    }

    /**
     * Scrolls to a certain row using only user-specified parameters.
     * 
     * @param rowIndex
     *            zero-based index of the row to scroll to.
     * @param destination
     *            desired destination placement of scrolled-to-row. See
     *            {@link ScrollDestination} for more information.
     * @param paddingPx
     *            number of pixels to overscroll. Behavior depends on
     *            destination.
     * @throws IllegalArgumentException
     *             if {@code destination} is {@link ScrollDestination#MIDDLE}
     *             and padding is nonzero, because having a padding on a
     *             centered row is undefined behavior, or if rowIndex is below
     *             zero or above the row count of the data source.
     */
    private void scrollToRow(int rowIndex, ScrollDestination destination,
            int paddingPx) throws IllegalArgumentException {
        int maxsize = escalator.getBody().getRowCount() - 1;

        if (rowIndex < 0) {
            throw new IllegalArgumentException("Row index (" + rowIndex
                    + ") is below zero!");
        }

        if (rowIndex > maxsize) {
            throw new IllegalArgumentException("Row index (" + rowIndex
                    + ") is above maximum (" + maxsize + ")!");
        }

        escalator.scrollToRow(rowIndex, destination, paddingPx);
    }

    /**
     * Scrolls to the beginning of the very first row.
     */
    public void scrollToStart() {
        scrollToRow(0, ScrollDestination.START);
    }

    /**
     * Scrolls to the end of the very last row.
     */
    public void scrollToEnd() {
        scrollToRow(escalator.getBody().getRowCount() - 1,
                ScrollDestination.END);
    }

    /**
     * Sets the vertical scroll offset.
     * 
     * @param px
     *            the number of pixels this grid should be scrolled down
     */
    public void setScrollTop(double px) {
        escalator.setScrollTop(px);
    }

    /**
     * Gets the vertical scroll offset
     * 
     * @return the number of pixels this grid is scrolled down
     */
    public double getScrollTop() {
        return escalator.getScrollTop();
    }

    /**
     * Gets the horizontal scroll offset
     * 
     * @return the number of pixels this grid is scrolled to the right
     */
    public double getScrollLeft() {
        return escalator.getScrollLeft();
    }

    private static final Logger getLogger() {
        return Logger.getLogger(Grid.class.getName());
    }

    /**
     * Sets the number of rows that should be visible in Grid's body, while
     * {@link #getHeightMode()} is {@link HeightMode#ROW}.
     * <p>
     * If Grid is currently not in {@link HeightMode#ROW}, the given value is
     * remembered, and applied once the mode is applied.
     * 
     * @param rows
     *            The height in terms of number of rows displayed in Grid's
     *            body. If Grid doesn't contain enough rows, white space is
     *            displayed instead.
     * @throws IllegalArgumentException
     *             if {@code rows} is zero or less
     * @throws IllegalArgumentException
     *             if {@code rows} is {@link Double#isInifinite(double)
     *             infinite}
     * @throws IllegalArgumentException
     *             if {@code rows} is {@link Double#isNaN(double) NaN}
     * 
     * @see #setHeightMode(HeightMode)
     */
    public void setHeightByRows(double rows) throws IllegalArgumentException {
        escalator.setHeightByRows(rows);
    }

    /**
     * Gets the amount of rows in Grid's body that are shown, while
     * {@link #getHeightMode()} is {@link HeightMode#ROW}.
     * <p>
     * By default, it is {@value Escalator#DEFAULT_HEIGHT_BY_ROWS}.
     * 
     * @return the amount of rows that should be shown in Grid's body, while in
     *         {@link HeightMode#ROW}.
     * @see #setHeightByRows(double)
     */
    public double getHeightByRows() {
        return escalator.getHeightByRows();
    }

    /**
     * Defines the mode in which the Grid widget's height is calculated.
     * <p>
     * If {@link HeightMode#CSS} is given, Grid will respect the values given
     * via {@link #setHeight(String)}, and behave as a traditional Widget.
     * <p>
     * If {@link HeightMode#ROW} is given, Grid will make sure that the body
     * will display as many rows as {@link #getHeightByRows()} defines.
     * <em>Note:</em> If headers/footers are inserted or removed, the widget
     * will resize itself to still display the required amount of rows in its
     * body. It also takes the horizontal scrollbar into account.
     * 
     * @param heightMode
     *            the mode in to which Grid should be set
     */
    public void setHeightMode(HeightMode heightMode) {
        /*
         * This method is a workaround for the fact that Vaadin re-applies
         * widget dimensions (height/width) on each state change event. The
         * original design was to have setHeight an setHeightByRow be equals,
         * and whichever was called the latest was considered in effect.
         * 
         * But, because of Vaadin always calling setHeight on the widget, this
         * approach doesn't work.
         */

        escalator.setHeightMode(heightMode);
    }

    /**
     * Returns the current {@link HeightMode} the Grid is in.
     * <p>
     * Defaults to {@link HeightMode#CSS}.
     * 
     * @return the current HeightMode
     */
    public HeightMode getHeightMode() {
        return escalator.getHeightMode();
    }

    private Set<String> getConsumedEventsForRenderer(Renderer<?> renderer) {
        Set<String> events = new HashSet<String>();
        if (renderer instanceof ComplexRenderer) {
            Collection<String> consumedEvents = ((ComplexRenderer<?>) renderer)
                    .getConsumedEvents();
            if (consumedEvents != null) {
                events.addAll(consumedEvents);
            }
        }
        return events;
    }

    @Override
    public void onBrowserEvent(Event event) {
        EventTarget target = event.getEventTarget();

        if (!Element.is(target)) {
            return;
        }

        Element e = Element.as(target);
        RowContainer container = escalator.findRowContainer(e);
        Cell cell;

        String eventType = event.getType();
        if (container == null) {
            if (eventType.equals(BrowserEvents.KEYDOWN)
                    || eventType.equals(BrowserEvents.KEYUP)
                    || eventType.equals(BrowserEvents.KEYPRESS)) {
                cell = cellFocusHandler.getFocusedCell();
                container = cellFocusHandler.containerWithFocus;
            } else {
                // Click in a location that does not contain cells.
                return;
            }
        } else {
            cell = container.getCell(e);
            if (eventType.equals(BrowserEvents.MOUSEDOWN)) {
                cellOnPrevMouseDown = cell;
            } else if (cell == null && eventType.equals(BrowserEvents.CLICK)) {
                /*
                 * Chrome has an interesting idea on click targets (see
                 * cellOnPrevMouseDown javadoc). Firefox, on the other hand, has
                 * the mousedown target as the click target.
                 */
                cell = cellOnPrevMouseDown;
            }
        }

        assert cell != null : "received " + eventType
                + "-event with a null cell target";

        // Editor Row can steal focus from Grid and is still handled
        if (handleEditorRowEvent(event, container, cell)) {
            return;
        }

        // Fire GridKeyEvents and pass the event to escalator.
        super.onBrowserEvent(event);

        if (!isElementInChildWidget(e)) {

            // Sorting through header Click / KeyUp
            if (handleHeaderDefaultRowEvent(event, container, cell)) {
                return;
            }

            if (handleRendererEvent(event, container, cell)) {
                return;
            }

            if (handleNavigationEvent(event, container, cell)) {
                return;
            }

            if (handleCellFocusEvent(event, container, cell)) {
                return;
            }
        }
    }

    private boolean isElementInChildWidget(Element e) {
        Widget w = Util.findWidget(e, null);

        if (w == this) {
            return false;
        }

        /*
         * If e is directly inside this grid, but the grid is wrapped in a
         * Composite, findWidget is not going to find this, only the wrapper.
         * Thus we need to check its parents to see if we encounter this; if we
         * don't, the found widget is actually a parent of this, so we should
         * return false.
         */
        while (w != null && w != this) {
            w = w.getParent();
        }
        return w != null;
    }

    private boolean handleEditorRowEvent(Event event, RowContainer container,
            Cell cell) {

        if (editorRow.getState() != State.INACTIVE) {
            if (event.getTypeInt() == Event.ONKEYDOWN
                    && event.getKeyCode() == EditorRow.KEYCODE_HIDE) {
                editorRow.cancel();
            }
            return true;
        }

        if (container == escalator.getBody() && editorRow.isEnabled()) {
            if (event.getTypeInt() == Event.ONDBLCLICK) {
                if (cell != null) {
                    editorRow.editRow(cell.getRow());
                    return true;
                }
            } else if (event.getTypeInt() == Event.ONKEYDOWN
                    && event.getKeyCode() == EditorRow.KEYCODE_SHOW) {
                editorRow.editRow(cellFocusHandler.rowWithFocus);
                return true;
            }
        }
        return false;
    }

    private boolean handleRendererEvent(Event event, RowContainer container,
            Cell cell) {

        if (container == escalator.getBody() && cell != null) {
            GridColumn<?, T> gridColumn = getColumnFromVisibleIndex(cell
                    .getColumn());
            boolean enterKey = event.getType().equals(BrowserEvents.KEYDOWN)
                    && event.getKeyCode() == KeyCodes.KEY_ENTER;
            boolean doubleClick = event.getType()
                    .equals(BrowserEvents.DBLCLICK);

            if (gridColumn.getRenderer() instanceof ComplexRenderer) {
                ComplexRenderer<?> cplxRenderer = (ComplexRenderer<?>) gridColumn
                        .getRenderer();
                if (cplxRenderer.getConsumedEvents().contains(event.getType())) {
                    if (cplxRenderer.onBrowserEvent(cell, event)) {
                        return true;
                    }
                }

                // Calls onActivate if KeyDown and Enter or double click
                if ((enterKey || doubleClick) && cplxRenderer.onActivate(cell)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean handleCellFocusEvent(Event event, RowContainer container,
            Cell cell) {
        Collection<String> navigation = cellFocusHandler.getNavigationEvents();
        if (navigation.contains(event.getType())) {
            cellFocusHandler.handleNavigationEvent(event, cell);
        }
        return false;
    }

    private boolean handleNavigationEvent(Event event, RowContainer unused,
            Cell cell) {
        if (!event.getType().equals(BrowserEvents.KEYDOWN)) {
            // Only handle key downs
            return false;
        }

        int newRow = -1;
        RowContainer container = escalator.getBody();
        switch (event.getKeyCode()) {
        case KeyCodes.KEY_HOME:
            if (container.getRowCount() > 0) {
                newRow = 0;
            }
            break;
        case KeyCodes.KEY_END:
            if (container.getRowCount() > 0) {
                newRow = container.getRowCount() - 1;
            }
            break;
        case KeyCodes.KEY_PAGEUP: {
            Range range = escalator.getVisibleRowRange();
            if (!range.isEmpty()) {
                int firstIndex = getFirstVisibleRowIndex();
                newRow = firstIndex - range.length();
                if (newRow < 0) {
                    newRow = 0;
                }
            }
            break;
        }
        case KeyCodes.KEY_PAGEDOWN: {
            Range range = escalator.getVisibleRowRange();
            if (!range.isEmpty()) {
                int lastIndex = getLastVisibleRowIndex();
                newRow = lastIndex + range.length();
                if (newRow >= container.getRowCount()) {
                    newRow = container.getRowCount() - 1;
                }
            }
            break;
        }
        default:
            return false;
        }

        scrollToRow(newRow);

        return true;
    }

    private Point rowEventTouchStartingPoint;
    private CellStyleGenerator<T> cellStyleGenerator;

    private boolean handleHeaderDefaultRowEvent(Event event,
            RowContainer container, final Cell cell) {
        if (container != escalator.getHeader()) {
            return false;
        }
        if (!getHeader().getRow(cell.getRow()).isDefault()) {
            return false;
        }
        if (!getColumn(cell.getColumn()).isSortable()) {
            // Only handle sorting events if the column is sortable
            return false;
        }

        if (BrowserEvents.TOUCHSTART.equals(event.getType())) {
            if (event.getTouches().length() > 1) {
                return false;
            }

            event.preventDefault();

            Touch touch = event.getChangedTouches().get(0);
            rowEventTouchStartingPoint = new Point(touch.getClientX(),
                    touch.getClientY());

            sorter.sortAfterDelay(GridConstants.LONG_TAP_DELAY, cell, true);

            return true;

        } else if (BrowserEvents.TOUCHMOVE.equals(event.getType())) {
            if (event.getTouches().length() > 1) {
                return false;
            }

            event.preventDefault();

            Touch touch = event.getChangedTouches().get(0);
            double diffX = Math.abs(touch.getClientX()
                    - rowEventTouchStartingPoint.getX());
            double diffY = Math.abs(touch.getClientY()
                    - rowEventTouchStartingPoint.getY());

            // Cancel long tap if finger strays too far from
            // starting point
            if (diffX > GridConstants.LONG_TAP_THRESHOLD
                    || diffY > GridConstants.LONG_TAP_THRESHOLD) {
                sorter.cancelDelayedSort();
            }

            return true;

        } else if (BrowserEvents.TOUCHEND.equals(event.getType())) {
            if (event.getTouches().length() > 1) {
                return false;
            }

            if (sorter.isDelayedSortScheduled()) {
                // Not a long tap yet, perform single sort
                sorter.cancelDelayedSort();
                sorter.sort(cell, false);
            }

            return true;

        } else if (BrowserEvents.TOUCHCANCEL.equals(event.getType())) {
            if (event.getTouches().length() > 1) {
                return false;
            }

            sorter.cancelDelayedSort();

            return true;

        } else if (BrowserEvents.CLICK.equals(event.getType())) {

            sorter.sort(cell, event.getShiftKey());

            // Click events should go onward to cell focus logic
            return false;
        } else {
            return false;
        }
    }

    @Override
    public com.google.gwt.user.client.Element getSubPartElement(String subPart) {
        // Parse SubPart string to type and indices
        String[] splitArgs = subPart.split("\\[");

        String type = splitArgs[0];
        int[] indices = new int[splitArgs.length - 1];
        for (int i = 0; i < indices.length; ++i) {
            String tmp = splitArgs[i + 1];
            indices[i] = Integer.parseInt(tmp.substring(0, tmp.length() - 1));
        }

        // Get correct RowContainer for type from Escalator
        RowContainer container = null;
        if (type.equalsIgnoreCase("header")) {
            container = escalator.getHeader();
        } else if (type.equalsIgnoreCase("cell")) {
            // If wanted row is not visible, we need to scroll there.
            Range visibleRowRange = escalator.getVisibleRowRange();
            if (indices.length > 0 && !visibleRowRange.contains(indices[0])) {
                try {
                    scrollToRow(indices[0]);
                } catch (IllegalArgumentException e) {
                    getLogger().log(Level.SEVERE, e.getMessage());
                }
                // Scrolling causes a lazy loading event. No element can
                // currently be retrieved.
                return null;
            }
            container = escalator.getBody();
        } else if (type.equalsIgnoreCase("footer")) {
            container = escalator.getFooter();
        }

        if (null != container) {
            if (indices.length == 0) {
                // No indexing. Just return the wanted container element
                return DOM.asOld(container.getElement());
            } else {
                try {
                    return DOM.asOld(getSubPart(container, indices));
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, e.getMessage());
                }
            }
        }
        return null;
    }

    private Element getSubPart(RowContainer container, int[] indices) {
        Element targetElement = container.getRowElement(indices[0]);

        // Scroll wanted column to view if able
        if (indices.length > 1 && targetElement != null) {
            if (escalator.getColumnConfiguration().getFrozenColumnCount() <= indices[1]) {
                escalator.scrollToColumn(indices[1], ScrollDestination.ANY, 0);
            }

            targetElement = getCellFromRow(TableRowElement.as(targetElement),
                    indices[1]);

            for (int i = 2; i < indices.length && targetElement != null; ++i) {
                targetElement = (Element) targetElement.getChild(indices[i]);
            }
        }

        return targetElement;
    }

    private Element getCellFromRow(TableRowElement rowElement, int index) {
        int childCount = rowElement.getCells().getLength();
        if (index < 0 || index >= childCount) {
            return null;
        }

        TableCellElement currentCell = null;
        boolean indexInColspan = false;
        int i = 0;

        while (!indexInColspan) {
            currentCell = rowElement.getCells().getItem(i);

            // Calculate if this is the cell we are looking for
            int colSpan = currentCell.getColSpan();
            indexInColspan = index < colSpan + i;

            // Increment by colspan to skip over hidden cells
            i += colSpan;
        }
        return currentCell;
    }

    @Override
    public String getSubPartName(com.google.gwt.user.client.Element subElement) {
        // Containers and matching SubPart types
        List<RowContainer> containers = Arrays.asList(escalator.getHeader(),
                escalator.getBody(), escalator.getFooter());
        List<String> containerType = Arrays.asList("header", "cell", "footer");

        for (int i = 0; i < containers.size(); ++i) {
            RowContainer container = containers.get(i);
            boolean containerRow = (subElement.getTagName().equalsIgnoreCase(
                    "tr") && subElement.getParentElement() == container
                    .getElement());
            if (containerRow) {
                // Wanted SubPart is row that is a child of containers root
                // To get indices, we use a cell that is a child of this row
                subElement = DOM.asOld(subElement.getFirstChildElement());
            }

            Cell cell = container.getCell(subElement);
            if (cell != null) {
                // Skip the column index if subElement was a child of root
                return containerType.get(i) + "[" + cell.getRow()
                        + (containerRow ? "]" : "][" + cell.getColumn() + "]");
            }
        }
        return null;
    }

    private void setSelectColumnRenderer(
            final Renderer<Boolean> selectColumnRenderer) {
        if (this.selectColumnRenderer == selectColumnRenderer) {
            return;
        }

        if (this.selectColumnRenderer != null) {
            removeColumnSkipSelectionColumnCheck(selectionColumn);
            cellFocusHandler.offsetRangeBy(-1);
        }

        this.selectColumnRenderer = selectColumnRenderer;

        if (selectColumnRenderer != null) {
            cellFocusHandler.offsetRangeBy(1);
            selectionColumn = new SelectionColumn(selectColumnRenderer);

            // FIXME: this needs to be done elsewhere, requires design...
            selectionColumn.setWidth(40);
            addColumnSkipSelectionColumnCheck(selectionColumn, 0);
            selectionColumn.initDone();
        } else {
            selectionColumn = null;
            refreshBody();
        }
    }

    /**
     * Sets the current selection model.
     * <p>
     * This function will call {@link SelectionModel#setGrid(Grid)}.
     * 
     * @param selectionModel
     *            a selection model implementation.
     * @throws IllegalArgumentException
     *             if selection model argument is null
     */
    public void setSelectionModel(SelectionModel<T> selectionModel) {

        if (selectionModel == null) {
            throw new IllegalArgumentException("Selection model can't be null");
        }

        if (selectColumnRenderer != null
                && selectColumnRenderer instanceof ComplexRenderer) {
            ((ComplexRenderer<?>) selectColumnRenderer).destroy();
        }

        this.selectionModel = selectionModel;
        selectionModel.setGrid(this);
        setSelectColumnRenderer(this.selectionModel
                .getSelectionColumnRenderer());
    }

    /**
     * Gets a reference to the current selection model.
     * 
     * @return the currently used SelectionModel instance.
     */
    public SelectionModel<T> getSelectionModel() {
        return selectionModel;
    }

    /**
     * Sets current selection mode.
     * <p>
     * This is a shorthand method for {@link Grid#setSelectionModel}.
     * 
     * @param mode
     *            a selection mode value
     * @see {@link SelectionMode}.
     */
    public void setSelectionMode(SelectionMode mode) {
        SelectionModel<T> model = mode.createModel();
        setSelectionModel(model);
    }

    /**
     * Test if a row is selected.
     * 
     * @param row
     *            a row object
     * @return true, if the current selection model considers the provided row
     *         object selected.
     */
    public boolean isSelected(T row) {
        return selectionModel.isSelected(row);
    }

    /**
     * Select a row using the current selection model.
     * <p>
     * Only selection models implementing {@link SelectionModel.Single} and
     * {@link SelectionModel.Multi} are supported; for anything else, an
     * exception will be thrown.
     * 
     * @param row
     *            a row object
     * @return <code>true</code> iff the current selection changed
     * @throws IllegalStateException
     *             if the current selection model is not an instance of
     *             {@link SelectionModel.Single} or {@link SelectionModel.Multi}
     */
    @SuppressWarnings("unchecked")
    public boolean select(T row) {
        if (selectionModel instanceof SelectionModel.Single<?>) {
            return ((SelectionModel.Single<T>) selectionModel).select(row);
        } else if (selectionModel instanceof SelectionModel.Multi<?>) {
            return ((SelectionModel.Multi<T>) selectionModel).select(row);
        } else {
            throw new IllegalStateException("Unsupported selection model");
        }
    }

    /**
     * Deselect a row using the current selection model.
     * <p>
     * Only selection models implementing {@link SelectionModel.Single} and
     * {@link SelectionModel.Multi} are supported; for anything else, an
     * exception will be thrown.
     * 
     * @param row
     *            a row object
     * @return <code>true</code> iff the current selection changed
     * @throws IllegalStateException
     *             if the current selection model is not an instance of
     *             {@link SelectionModel.Single} or {@link SelectionModel.Multi}
     */
    @SuppressWarnings("unchecked")
    public boolean deselect(T row) {
        if (selectionModel instanceof SelectionModel.Single<?>) {
            return ((SelectionModel.Single<T>) selectionModel).deselect(row);
        } else if (selectionModel instanceof SelectionModel.Multi<?>) {
            return ((SelectionModel.Multi<T>) selectionModel).deselect(row);
        } else {
            throw new IllegalStateException("Unsupported selection model");
        }
    }

    /**
     * Gets last selected row from the current SelectionModel.
     * <p>
     * Only selection models implementing {@link SelectionModel.Single} are
     * valid for this method; for anything else, use the
     * {@link Grid#getSelectedRows()} method.
     * 
     * @return a selected row reference, or null, if no row is selected
     * @throws IllegalStateException
     *             if the current selection model is not an instance of
     *             {@link SelectionModel.Single}
     */
    public T getSelectedRow() {
        if (selectionModel instanceof SelectionModel.Single<?>) {
            return ((SelectionModel.Single<T>) selectionModel).getSelectedRow();
        } else {
            throw new IllegalStateException(
                    "Unsupported selection model; can not get single selected row");
        }
    }

    /**
     * Gets currently selected rows from the current selection model.
     * 
     * @return a non-null collection containing all currently selected rows.
     */
    public Collection<T> getSelectedRows() {
        return selectionModel.getSelectedRows();
    }

    @Override
    public HandlerRegistration addSelectionChangeHandler(
            final SelectionChangeHandler<T> handler) {
        return addHandler(handler, SelectionChangeEvent.getType());
    }

    /**
     * Sets the current sort order using the fluid Sort API. Read the
     * documentation for {@link Sort} for more information.
     * 
     * @param s
     *            a sort instance
     */
    public void sort(Sort s) {
        setSortOrder(s.build());
    }

    /**
     * Sorts the Grid data in ascending order along one column.
     * 
     * @param column
     *            a grid column reference
     */
    public <C> void sort(GridColumn<C, T> column) {
        sort(column, SortDirection.ASCENDING);
    }

    /**
     * Sorts the Grid data along one column.
     * 
     * @param column
     *            a grid column reference
     * @param direction
     *            a sort direction value
     */
    public <C> void sort(GridColumn<C, T> column, SortDirection direction) {
        sort(Sort.by(column, direction));
    }

    /**
     * Sets the sort order to use. Setting this causes the Grid to re-sort
     * itself.
     * 
     * @param order
     *            a sort order list. If set to null, the sort order is cleared.
     */
    public void setSortOrder(List<SortOrder> order) {
        setSortOrder(order, SortEventOriginator.API);
    }

    private void setSortOrder(List<SortOrder> order,
            SortEventOriginator originator) {
        if (order != sortOrder) {
            sortOrder.clear();
            if (order != null) {
                sortOrder.addAll(order);
            }
        }
        sort(originator);
    }

    /**
     * Get a copy of the current sort order array.
     * 
     * @return a copy of the current sort order array
     */
    public List<SortOrder> getSortOrder() {
        return Collections.unmodifiableList(sortOrder);
    }

    /**
     * Finds the sorting order for this column
     */
    private SortOrder getSortOrder(GridColumn<?, ?> column) {
        for (SortOrder order : getSortOrder()) {
            if (order.getColumn() == column) {
                return order;
            }
        }
        return null;
    }

    /**
     * Register a GWT event handler for a sorting event. This handler gets
     * called whenever this Grid needs its data source to provide data sorted in
     * a specific order.
     * 
     * @param handler
     *            a sort event handler
     * @return the registration for the event
     */
    public HandlerRegistration addSortHandler(SortEventHandler<T> handler) {
        return addHandler(handler, SortEvent.getType());
    }

    /**
     * Register a GWT event handler for a data available event. This handler
     * gets called whenever the {@link DataSource} for this Grid has new data
     * available.
     * <p>
     * This handle will be fired with the current available data after
     * registration is done.
     * 
     * @param handler
     *            a data available event handler
     * @return the registartion for the event
     */
    public HandlerRegistration addDataAvailableHandler(
            final DataAvailableHandler handler) {
        // Deferred call to handler with current row range
        Scheduler.get().scheduleFinally(new ScheduledCommand() {
            @Override
            public void execute() {
                handler.onDataAvailable(new DataAvailableEvent(
                        currentDataAvailable));
            }
        });
        return addHandler(handler, DataAvailableEvent.TYPE);
    }

    /**
     * Register a BodyKeyDownHandler to this Grid. The event for this handler is
     * fired when a KeyDown event occurs while cell focus is in the Body of this
     * Grid.
     * 
     * @param handler
     *            the key handler to register
     * @return the registration for the event
     */
    public HandlerRegistration addBodyKeyDownHandler(BodyKeyDownHandler handler) {
        return addHandler(handler, keyDown.getAssociatedType());
    }

    /**
     * Register a BodyKeyUpHandler to this Grid. The event for this handler is
     * fired when a KeyUp event occurs while cell focus is in the Body of this
     * Grid.
     * 
     * @param handler
     *            the key handler to register
     * @return the registration for the event
     */
    public HandlerRegistration addBodyKeyUpHandler(BodyKeyUpHandler handler) {
        return addHandler(handler, keyUp.getAssociatedType());
    }

    /**
     * Register a BodyKeyPressHandler to this Grid. The event for this handler
     * is fired when a KeyPress event occurs while cell focus is in the Body of
     * this Grid.
     * 
     * @param handler
     *            the key handler to register
     * @return the registration for the event
     */
    public HandlerRegistration addBodyKeyPressHandler(
            BodyKeyPressHandler handler) {
        return addHandler(handler, keyPress.getAssociatedType());
    }

    /**
     * Register a HeaderKeyDownHandler to this Grid. The event for this handler
     * is fired when a KeyDown event occurs while cell focus is in the Header of
     * this Grid.
     * 
     * @param handler
     *            the key handler to register
     * @return the registration for the event
     */
    public HandlerRegistration addHeaderKeyDownHandler(
            HeaderKeyDownHandler handler) {
        return addHandler(handler, keyDown.getAssociatedType());
    }

    /**
     * Register a HeaderKeyUpHandler to this Grid. The event for this handler is
     * fired when a KeyUp event occurs while cell focus is in the Header of this
     * Grid.
     * 
     * @param handler
     *            the key handler to register
     * @return the registration for the event
     */
    public HandlerRegistration addHeaderKeyUpHandler(HeaderKeyUpHandler handler) {
        return addHandler(handler, keyUp.getAssociatedType());
    }

    /**
     * Register a HeaderKeyPressHandler to this Grid. The event for this handler
     * is fired when a KeyPress event occurs while cell focus is in the Header
     * of this Grid.
     * 
     * @param handler
     *            the key handler to register
     * @return the registration for the event
     */
    public HandlerRegistration addHeaderKeyPressHandler(
            HeaderKeyPressHandler handler) {
        return addHandler(handler, keyPress.getAssociatedType());
    }

    /**
     * Register a FooterKeyDownHandler to this Grid. The event for this handler
     * is fired when a KeyDown event occurs while cell focus is in the Footer of
     * this Grid.
     * 
     * @param handler
     *            the key handler to register
     * @return the registration for the event
     */
    public HandlerRegistration addFooterKeyDownHandler(
            FooterKeyDownHandler handler) {
        return addHandler(handler, keyDown.getAssociatedType());
    }

    /**
     * Register a FooterKeyUpHandler to this Grid. The event for this handler is
     * fired when a KeyUp event occurs while cell focus is in the Footer of this
     * Grid.
     * 
     * @param handler
     *            the key handler to register
     * @return the registration for the event
     */
    public HandlerRegistration addFooterKeyUpHandler(FooterKeyUpHandler handler) {
        return addHandler(handler, keyUp.getAssociatedType());
    }

    /**
     * Register a FooterKeyPressHandler to this Grid. The event for this handler
     * is fired when a KeyPress event occurs while cell focus is in the Footer
     * of this Grid.
     * 
     * @param handler
     *            the key handler to register
     * @return the registration for the event
     */
    public HandlerRegistration addFooterKeyPressHandler(
            FooterKeyPressHandler handler) {
        return addHandler(handler, keyPress.getAssociatedType());
    }

    /**
     * Apply sorting to data source.
     */
    private void sort(SortEventOriginator originator) {
        refreshHeader();
        fireEvent(new SortEvent<T>(this,
                Collections.unmodifiableList(sortOrder), originator));
    }

    private int getLastVisibleRowIndex() {
        int lastRowIndex = escalator.getVisibleRowRange().getEnd();
        int footerTop = escalator.getFooter().getElement().getAbsoluteTop();
        Element lastRow;

        do {
            lastRow = escalator.getBody().getRowElement(--lastRowIndex);
        } while (lastRow.getAbsoluteBottom() > footerTop);

        return lastRowIndex;
    }

    private int getFirstVisibleRowIndex() {
        int firstRowIndex = escalator.getVisibleRowRange().getStart();
        int headerBottom = escalator.getHeader().getElement()
                .getAbsoluteBottom();
        Element firstRow = escalator.getBody().getRowElement(firstRowIndex);

        while (firstRow.getAbsoluteTop() < headerBottom) {
            firstRow = escalator.getBody().getRowElement(++firstRowIndex);
        }

        return firstRowIndex;
    }

    /**
     * Adds a scroll handler to this grid
     * 
     * @param handler
     *            the scroll handler to add
     * @return a handler registration for the registered scroll handler
     */
    public HandlerRegistration addScrollHandler(ScrollHandler handler) {
        return addHandler(handler, ScrollEvent.TYPE);
    }

    @Override
    public boolean isWorkPending() {
        return escalator.isWorkPending() || dataIsBeingFetched;
    }

    /**
     * Sets a new column order for the grid. All columns which are not ordered
     * here will remain in the order they were before as the last columns of
     * grid.
     * 
     * @param orderedColumns
     *            array of columns in wanted order
     */
    public void setColumnOrder(GridColumn<?, T>... orderedColumns) {
        List<GridColumn<?, T>> newOrder = new ArrayList<GridColumn<?, T>>();
        if (selectionColumn != null) {
            newOrder.add(selectionColumn);
        }

        int i = 0;
        for (GridColumn<?, T> column : orderedColumns) {
            if (columns.contains(column)) {
                newOrder.add(column);
                ++i;
            } else {
                throw new IllegalArgumentException("Given column at index " + i
                        + " does not exist in Grid");
            }
        }

        if (columns.size() != newOrder.size()) {
            columns.removeAll(newOrder);
            newOrder.addAll(columns);
        }
        columns = newOrder;

        // Update column widths.
        for (GridColumn<?, T> column : columns) {
            column.reapplyWidth();
        }

        // Recalculate all the colspans
        for (HeaderRow row : header.getRows()) {
            row.calculateColspans();
        }
        for (FooterRow row : footer.getRows()) {
            row.calculateColspans();
        }

        refreshHeader();
        refreshBody();
        refreshFooter();
    }

    /**
     * Sets the cell style generator that is used for generating styles for rows
     * and cells.
     * 
     * @param cellStyleGenerator
     *            the cell style generator to set, or <code>null</code> to
     *            remove a previously set generator
     */
    public void setCellStyleGenerator(CellStyleGenerator<T> cellStyleGenerator) {
        this.cellStyleGenerator = cellStyleGenerator;
        refreshBody();
    }

    /**
     * Gets the cell style generator that is used for generating styles for rows
     * and cells.
     * 
     * @return the cell style generator, or <code>null</code> if no generator is
     *         set
     */
    public CellStyleGenerator<T> getCellStyleGenerator() {
        return cellStyleGenerator;
    }

    private static void setCustomStyleName(Element element, String styleName) {
        assert element != null;

        String oldStyleName = element
                .getPropertyString(CUSTOM_STYLE_PROPERTY_NAME);

        if (!SharedUtil.equals(oldStyleName, styleName)) {
            if (oldStyleName != null) {
                element.removeClassName(oldStyleName);
            }
            if (styleName != null) {
                element.addClassName(styleName);
            }
            element.setPropertyString(CUSTOM_STYLE_PROPERTY_NAME, styleName);
        }

    }
}
