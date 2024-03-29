package com.example.exceldemo.util;

import com.example.exceldemo.annota.ExcelAnnotation;
import com.example.exceldemo.common.Const;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.hssf.util.HSSFRegionUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;



/**
 * 导出excel文件工具类
 *
 * @author huxiaolong
 * @date 2018/8/22
 */
public class ExcelUtil {
    private static Logger logger = LoggerFactory.getLogger(ExcelUtil.class);

    /**
     * 接口方法
     * @param tableName 表名，添加在表头列之上
     * @param dataSet 从数据库中获取的对象集合
     * @param <E>
     * @return 包含完整数据和指定样式的HSSFWorkbook对象
     */
    public static <E> HSSFWorkbook exportExcel(String tableName, List<E> dataSet) {
        HSSFWorkbook hssfWorkbook = null;
        if (dataSet.size() > 0) {
            // 行索引，首行索引为0
            int rowIndex = 0;
            // 获取sheet信息
            Map<String, Object> sheetInfoMap = ExcelUtil.getSheetInfo(dataSet.get(0));
            String sheetName = (String) sheetInfoMap.get("sheetName");
            String datePattern = (String) sheetInfoMap.get("datePattern");
            boolean counted = (boolean) sheetInfoMap.get("counted");
            // 组装表头信息，已经依据cell的index进行过排序操作
            List<HeaderNode> headerNodeList = ExcelUtil.assembleHeaderNode(dataSet.get(0));
            if (!headerNodeList.isEmpty()) {
                // 如果有序号列，则进行下面的操作，添加序号列节点到HeaderNodeList首位
                if (counted) {
                    ExcelUtil.increasePreNotExtensibleHeaderNodeSum(headerNodeList);
                    HeaderNode headerNodeForCountColumn = new HeaderNode();
                    headerNodeForCountColumn.setHeaderName(Const.COUNT);
                    headerNodeForCountColumn.setIndex(-1);
                    headerNodeForCountColumn.setLevel(0);
                    headerNodeForCountColumn.setExtensible(false);
                    headerNodeForCountColumn.setPreNotExtensibleHeaderNodeSum(0);
                    headerNodeForCountColumn.setSubNotExtensibleHeaderNodeSum(0);
                    headerNodeList.add(0, headerNodeForCountColumn);
                }
                // 获取不可扩展cell（即就是子cell）的名称序列，因assembleHeaderNode方法的排序操作，此序列中cell名称同样为顺序存储
                List<String> notExtensibleHeaderNameList = ExcelUtil.getNotExtensibleHeaderNameList(headerNodeList);
                // 获得表头区域列数
                int columnSum = notExtensibleHeaderNameList.size();
                hssfWorkbook = new HSSFWorkbook();
                HSSFSheet hssfSheet = hssfWorkbook.createSheet(sheetName);
                /*1.标题行设置*/
                HSSFRow hssfRow = hssfSheet.createRow(rowIndex);
                ExcelUtil.setRowHeight(Const.RowType.TABLE_NAME, hssfRow);
                CellRangeAddress cellRangeAddress = new CellRangeAddress(rowIndex, rowIndex, 0, columnSum - 1);
                hssfSheet.addMergedRegion(cellRangeAddress);
                Cell cell = CellUtil.createCell(hssfRow, 0, tableName);
                ExcelUtil.setCellStyleForMergeRegion(cellRangeAddress, cell, Const.RowType.TABLE_NAME, hssfSheet, hssfWorkbook);
                rowIndex++;
                /*2.表头行设置*/
                List<HSSFRow> headerRowList = ExcelUtil.createHeaderRows(headerNodeList, hssfSheet, hssfWorkbook, rowIndex);
                rowIndex += headerRowList.size();
                /*3.内容行设置*/
                ExcelUtil.createContentRows(rowIndex, counted, columnSum, datePattern, headerNodeList, dataSet, hssfSheet, hssfWorkbook);
            }
        }
        return hssfWorkbook;
    }

    private static <E> Map<String, Object> getSheetInfo(E e) {
        Map<String, Object> sheetInfoMap = new HashMap<>(8);
        if (e != null) {
            ExcelAnnotation excelAnnotation = (ExcelAnnotation) e.getClass().getAnnotations()[0];
            sheetInfoMap.put("sheetName", excelAnnotation.sheetName());
            sheetInfoMap.put("datePattern", excelAnnotation.datePattern());
            sheetInfoMap.put("counted", excelAnnotation.counted());
        }
        logger.info("sheet信息：" + sheetInfoMap.toString());
        return sheetInfoMap;
    }

    private static <E> List<HeaderNode> assembleHeaderNode(E e) {
        List<HeaderInfo> headerInfoList = ExcelUtil.getHeaderInfo(e);
        logger.info("headerInfoList:" + headerInfoList.toString());
        List<HeaderNode> headerNodeList = new ArrayList<>();
        HeaderNode headerNode;
        if (headerInfoList.size() > 0) {
            for (HeaderInfo item : headerInfoList) {
                headerNode = new HeaderNode();
                headerNode.setHeaderName(item.getHeaderName());
                headerNode.setIndex(item.getIndex());
                headerNode.setLevel(item.getLevel());
                headerNode.setParentIndex(item.getParentIndex());
                headerNode.setExtensible(isExtensible(item, headerInfoList));
                headerNode.setPreNotExtensibleHeaderNodeSum(getPreNotExtensibleHeaderNodeSum(item, headerInfoList));
                headerNode.setSubNotExtensibleHeaderNodeSum(getSubNotExtensibleHeaderNodeSum(item, headerInfoList));
                headerNodeList.add(headerNode);
            }
        }
        logger.info("headerNodeList:" + headerNodeList.toString());
        return headerNodeList;
    }

    private static void increasePreNotExtensibleHeaderNodeSum(List<HeaderNode> headerNodeList) {
        for (HeaderNode item : headerNodeList) {
            item.setPreNotExtensibleHeaderNodeSum(item.getPreNotExtensibleHeaderNodeSum() + 1);
        }
    }

    private static List<String> getNotExtensibleHeaderNameList(List<HeaderNode> headerNodeList) {
        List<String> notExtensibleHeaderNameList = new ArrayList<>();
        if (headerNodeList.size() > 0) {
            for (HeaderNode item : headerNodeList) {
                if (!item.isExtensible()) {
                    notExtensibleHeaderNameList.add(item.getHeaderName());
                }
            }
        }
        return notExtensibleHeaderNameList;
    }

    /**
     * 设置行高
     * @param rowType 行的类型，如表名行、表头行、内容行
     * @param hssfRow 行对象
     */
    private static void setRowHeight(String rowType, HSSFRow hssfRow) {
        if (Const.RowType.TABLE_NAME.equals(rowType)) {
            hssfRow.setHeight((short) (36 * 20));
        } else if (Const.RowType.TABLE_HEADER.equals(rowType)) {
            hssfRow.setHeight((short) (20 * 20));
        } else if (Const.RowType.TABLE_CONTENT.equals(rowType)) {
            hssfRow.setHeight((short) (20 * 20));
        } else {
            hssfRow.setHeight((short) (20 * 20));
        }
    }

    /**
     * 设置跨行跨列区域的字体及边框样式
     * 注意：跨行跨列区域样式设置有别与单个cell样式设置
     * @param cellRangeAddress 跨行范围对象
     * @param cell cell对象，第一列
     * @param cellType 单元格类型
     * @param hssfSheet sheet页
     * @param hssfWorkbook excel文档对象
     */
    private static void setCellStyleForMergeRegion(CellRangeAddress cellRangeAddress, Cell cell, String cellType,
                                                   HSSFSheet hssfSheet, HSSFWorkbook hssfWorkbook) {
        // 创建样式对象
        HSSFCellStyle hssfCellStyle = hssfWorkbook.createCellStyle();
        // 创建字体对象
        HSSFFont hssfFont = hssfWorkbook.createFont();
        // 设置字体及其高度
        ExcelUtil.setFont(cellType, hssfFont);
        // 设置单元格样式字体属性
        hssfCellStyle.setFont(hssfFont);
        // 水平居中
        hssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
        // 垂直居中
        hssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // 添加样式到单元格
        cell.setCellStyle(hssfCellStyle);
        // 设置跨行区域边框
        HSSFRegionUtil.setBorderTop(0x1, cellRangeAddress, hssfSheet, hssfWorkbook);
        HSSFRegionUtil.setBorderLeft(0x1, cellRangeAddress, hssfSheet, hssfWorkbook);
        HSSFRegionUtil.setBorderRight(0x1, cellRangeAddress, hssfSheet, hssfWorkbook);
        HSSFRegionUtil.setBorderBottom(0x1, cellRangeAddress, hssfSheet, hssfWorkbook);
    }

    /**
     * 生成表头行，支持单行与多行表头，（无排序要求）
     * @param headerNodeList
     * @param hssfSheet
     * @param hssfWorkbook
     * @param startIndex
     * @return
     */
    private static List<HSSFRow> createHeaderRows(List<HeaderNode> headerNodeList, HSSFSheet hssfSheet, HSSFWorkbook hssfWorkbook, int startIndex) {
        List<HSSFRow> headerRowList = new ArrayList<>();
        if (headerNodeList.size() > 0) {
            int deep = ExcelUtil.getHeaderDeep(headerNodeList);
            HSSFRow hssfRow;
            for (int i = 0; i < deep + 1; i++) {
                hssfRow = hssfSheet.createRow(startIndex + i);
                ExcelUtil.setRowHeight(Const.RowType.TABLE_HEADER, hssfRow);
                headerRowList.add(hssfRow);
            }
            String headerName;
            int level;
            int preNotExtensibleHeaderNodeSum;
            int subNotExtensibleHeaderNodeSum;
            CellRangeAddress cellRangeAddress;
            Cell cell;
            for (HeaderNode item : headerNodeList) {
                headerName = item.getHeaderName();
                level = item.getLevel();
                preNotExtensibleHeaderNodeSum = item.getPreNotExtensibleHeaderNodeSum();
                subNotExtensibleHeaderNodeSum = item.getSubNotExtensibleHeaderNodeSum();
                if (item.isExtensible()) {
                    cellRangeAddress = new CellRangeAddress(startIndex + level, startIndex + level,
                            preNotExtensibleHeaderNodeSum, preNotExtensibleHeaderNodeSum + subNotExtensibleHeaderNodeSum - 1);
                } else {
                    cellRangeAddress = new CellRangeAddress(startIndex + level,
                            startIndex + deep, preNotExtensibleHeaderNodeSum, preNotExtensibleHeaderNodeSum);
                }
                hssfSheet.addMergedRegion(cellRangeAddress);
                cell = CellUtil.createCell(headerRowList.get(level), preNotExtensibleHeaderNodeSum, headerName);
                ExcelUtil.setCellStyleForMergeRegion(cellRangeAddress, cell, Const.RowType.TABLE_HEADER, hssfSheet, hssfWorkbook);
            }
        }
        return headerRowList;
    }

    private static <E> void createContentRows(int startIndex, boolean counted, int columnSum, String pattern, List<HeaderNode> headerNodeList,
                                              List<E> dataSet, HSSFSheet hssfSheet, HSSFWorkbook hssfWorkbook) {
        HSSFRow hssfRow;
        HSSFCell[] hssfCells = new HSSFCell[columnSum];
        for (int i = 0; i < dataSet.size(); i++) {
            hssfRow = hssfSheet.createRow(i + startIndex);
            ExcelUtil.setRowHeight(Const.RowType.TABLE_CONTENT, hssfRow);
            Object temp = dataSet.get(i);
            List<Object> fieldValueList = ExcelUtil.getRowValueList(temp, headerNodeList);
            if (i == 0) {
                logger.info("首行单元值序列：" + fieldValueList.toString());
            }
            Object cellValue;
            int k = 0;
            if (counted) {
                hssfCells[0] = hssfRow.createCell(0);
                ExcelUtil.setCellStyle(hssfWorkbook, hssfCells[0], Const.RowType.TABLE_CONTENT);
                hssfCells[0].setCellValue(i + 1);
                hssfSheet.autoSizeColumn(0, true);
                k++;
            }
            for (int j = k; j < columnSum; j++) {
                hssfCells[j] = hssfRow.createCell(j);
                ExcelUtil.setCellStyle(hssfWorkbook, hssfCells[j], Const.RowType.TABLE_CONTENT);
                cellValue = fieldValueList.get(j - k);
                if (cellValue == null) {
                    hssfCells[j].setCellValue("");
                } else if (cellValue instanceof Integer) {
                    hssfCells[j].setCellValue((Integer) cellValue);
                } else if (cellValue instanceof Long) {
                    hssfCells[j].setCellValue((Long) cellValue);
                } else if (cellValue instanceof Double) {
                    hssfCells[j].setCellValue((Double) cellValue);
                } else if (cellValue instanceof Float) {
                    hssfCells[j].setCellValue((Float) cellValue);
                } else if (cellValue instanceof Boolean) {
                    hssfCells[j].setCellValue((Boolean) cellValue ? "是" : "否");
                } else if (cellValue instanceof Date) {
                    hssfCells[j].setCellValue(new SimpleDateFormat(StringUtils.isNotBlank(pattern) ? pattern : "yyyy-MM-dd").format((Date) cellValue));
                } else {
                    hssfCells[j].setCellValue(String.valueOf(cellValue));
                }
                hssfSheet.autoSizeColumn(j, true);
            }
        }
    }

    private static <E> List<HeaderInfo> getHeaderInfo(E e) {
        Map<Integer, HeaderInfo> map = new TreeMap<>();
        if (e != null) {
            Field[] fields = e.getClass().getDeclaredFields();
            HeaderInfo headerInfo;
            ExcelAnnotation excelAnnotation;
            for (Field field : fields) {
                if (field.getAnnotations().length != 0) {
                    headerInfo = new HeaderInfo();
                    excelAnnotation = (ExcelAnnotation) field.getAnnotations()[0];
                    headerInfo.setHeaderName(excelAnnotation.headerName());
                    headerInfo.setIndex(excelAnnotation.index());
                    headerInfo.setLevel(excelAnnotation.level());
                    headerInfo.setParentIndex(excelAnnotation.parentIndex());
                    map.put(excelAnnotation.index(), headerInfo);
                }
            }
        }
        return ExcelUtil.sortByKey(map);
    }

    /**
     * 判断节点是否可扩展，即对应表头下是否含有子表头，也可以调用getSubNotExtensibleHeaderNodeSum方法，
     * 通过判断其值是否为零来判断当前节点是否可扩展
     * @param headerInfo
     * @param headerInfoList
     * @return
     */
    private static boolean isExtensible(HeaderInfo headerInfo, List<HeaderInfo> headerInfoList) {
        for (HeaderInfo item : headerInfoList) {
            if (headerInfo.getIndex() == item.getParentIndex()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获得当前节点之前（或者空间左侧）不可扩展节点的数量，
     * 前提是headerInfoList中表头单元信息对象存储的先后顺序，需遵循基于对应表头单元空间位置先左后右，从上往下（或先父后子）的原则
     * 反映在headerInfoList中就是其元素以index由小到大顺序存储，（getHeaderInfo方法中已做此处理）
     * @param headerInfo
     * @param headerInfoList
     * @return
     */
    private static int getPreNotExtensibleHeaderNodeSum(HeaderInfo headerInfo, List<HeaderInfo> headerInfoList) {
        int count = 0;
        for (int i = 0; i < headerInfoList.size(); i++) {
            if (headerInfo.equals(headerInfoList.get(i))) {
                break;
            }
            if (!isExtensible(headerInfoList.get(i), headerInfoList)) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获得当前节点下侧不可扩展节点的数量，（无排序要求）
     * @param headerInfo
     * @param headerInfoList
     * @return
     */
    private static int getSubNotExtensibleHeaderNodeSum(HeaderInfo headerInfo, List<HeaderInfo> headerInfoList) {
        int count = 0;
        for (int i = 0; i < headerInfoList.size(); i++) {
            if (headerInfo.getIndex() == headerInfoList.get(i).getParentIndex()) {
                if (!isExtensible(headerInfoList.get(i), headerInfoList)) {
                    count++;
                } else {
                    count += getSubNotExtensibleHeaderNodeSum(headerInfoList.get(i), headerInfoList);
                }
            }
        }
        return count;
    }

    private static void setFont(String cellType, HSSFFont hssfFont) {
        if (hssfFont != null) {
            if (Const.RowType.TABLE_NAME.equals(cellType)) {
                hssfFont.setFontName("宋体");
                hssfFont.setFontHeightInPoints((short) 18);
            } else if (Const.RowType.TABLE_HEADER.equals(cellType)) {
                hssfFont.setFontName("宋体");
                hssfFont.setFontHeightInPoints((short) 11);
            } else if (Const.RowType.TABLE_CONTENT.equals(cellType)) {
                hssfFont.setFontName("宋体");
                hssfFont.setFontHeightInPoints((short) 10);
            } else {
                // 设置字体类型
                hssfFont.setFontName("宋体");
                // 设置字体高度
                hssfFont.setFontHeightInPoints((short) 9);
                // 设置字体颜色和下划线样式
                hssfFont.setColor(HSSFFont.COLOR_RED);
                hssfFont.setUnderline((byte) 1);
            }
        }
    }

    private static int getHeaderDeep(List<HeaderNode> headerNodeList) {
        int deep = 0;
        for (HeaderNode item : headerNodeList) {
            if (item.getLevel() > deep) {
                deep = item.getLevel();
            }
        }
        return deep;
    }

    private static List<Object> getRowValueList(Object temp, List<HeaderNode> headerNodeList) {
        List<Integer> indexList = ExcelUtil.getNotExtensibleHeaderIndexList(headerNodeList);
        Map<Integer, Object> map = new TreeMap<>();
        Field[] fields = temp.getClass().getDeclaredFields();
        ExcelAnnotation excelAnnotation;
        int index;
        boolean valueColumn;
        String fieldName;
        String getMethodName;
        Object value = null;
        for (Field field : fields) {
            if (field.getAnnotations().length != 0) {
                excelAnnotation = (ExcelAnnotation) field.getAnnotations()[0];
                index = excelAnnotation.index();
                valueColumn = ExcelUtil.isValueColumn(index, indexList);
                if (valueColumn) {
                    fieldName = field.getName();
                    getMethodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                    try {
                        Method method = temp.getClass().getMethod(getMethodName);
                        value = method.invoke(temp);
                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    map.put(index, value);
                }
            }
        }
        return ExcelUtil.sortByKey(map);
    }

    /**
     * 设置单元格及内部字体样式
     * @param hssfWorkbook hssfWorkbook对象
     * @param hssfCell 单元格对象
     * @param cellType 单元格类型
     */
    private static void setCellStyle(HSSFWorkbook hssfWorkbook, HSSFCell hssfCell, String cellType) {
        // 创建样式对象
        HSSFCellStyle hssfCellStyle = hssfWorkbook.createCellStyle();
        // 创建字体对象
        HSSFFont hssfFont = hssfWorkbook.createFont();
        // 设置单元格样式字体属性
        hssfCellStyle.setFont(hssfFont);
        // 水平居中
        hssfCellStyle.setAlignment(HorizontalAlignment.CENTER);
        // 垂直居中
        hssfCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // 单元格表框线类型，BORDER_THIN为实线，BORDER_DOTTED为点划线
        hssfCellStyle.setBorderTop(BorderStyle.THIN);
        hssfCellStyle.setBorderRight(BorderStyle.THIN);
        hssfCellStyle.setBorderBottom(BorderStyle.THIN);
        hssfCellStyle.setBorderLeft(BorderStyle.THIN);
        // 设置字体及其高度
        ExcelUtil.setFont(cellType, hssfFont);
        // 为单元格添加已设置完整的样式
        hssfCell.setCellStyle(hssfCellStyle);
    }

    private static List<Integer> getNotExtensibleHeaderIndexList(List<HeaderNode> headerNodeList) {
        List<Integer> notExtensibleHeaderIndexList = new ArrayList<>();
        for (HeaderNode item : headerNodeList) {
            if (!item.isExtensible() && item.getIndex() >= 0) {
                notExtensibleHeaderIndexList.add(item.getIndex());
            }
        }
        return notExtensibleHeaderIndexList;
    }

    private static boolean isValueColumn(int index, List<Integer> indexList) {
        for (Integer item : indexList) {
            if (index == item) {
                return true;
            }
        }
        return false;
    }

    private static <E> List<E> sortByKey(Map<Integer, E> map) {
        List<E> list = new ArrayList<>();
        Set<Integer> keySet = map.keySet();
        for (Integer key : keySet) {
            list.add(map.get(key));
        }
        return list;
    }

    static class HeaderInfo {
        private String headerName;
        private int index;
        private int level;
        private int parentIndex;

        public HeaderInfo() {
        }

        public HeaderInfo(String headerName, int index, int level, int parentIndex) {
            this.headerName = headerName;
            this.index = index;
            this.level = level;
            this.parentIndex = parentIndex;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int getParentIndex() {
            return parentIndex;
        }

        public void setParentIndex(int parentIndex) {
            this.parentIndex = parentIndex;
        }

        @Override
        public String toString() {
            return "HeaderInfo{" +
                    "headerName='" + headerName + '\'' +
                    ", index=" + index +
                    ", level=" + level +
                    ", parentIndex=" + parentIndex +
                    '}';
        }
    }

    static class HeaderNode {
        private String headerName;
        private int index;
        private int level;
        private int parentIndex;
        private boolean extensible;
        private int preNotExtensibleHeaderNodeSum;
        private int subNotExtensibleHeaderNodeSum;

        public HeaderNode() {
        }

        public HeaderNode(String headerName, int index, int level, int parentIndex, boolean extensible,
                          int preNotExtensibleHeaderNodeSum, int subNotExtensibleHeaderNodeSum) {
            this.headerName = headerName;
            this.index = index;
            this.level = level;
            this.parentIndex = parentIndex;
            this.extensible = extensible;
            this.preNotExtensibleHeaderNodeSum = preNotExtensibleHeaderNodeSum;
            this.subNotExtensibleHeaderNodeSum = subNotExtensibleHeaderNodeSum;
        }

        public String getHeaderName() {
            return headerName;
        }

        public void setHeaderName(String headerName) {
            this.headerName = headerName;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public int getParentIndex() {
            return parentIndex;
        }

        public void setParentIndex(int parentIndex) {
            this.parentIndex = parentIndex;
        }

        public boolean isExtensible() {
            return extensible;
        }

        public void setExtensible(boolean extensible) {
            this.extensible = extensible;
        }

        public int getPreNotExtensibleHeaderNodeSum() {
            return preNotExtensibleHeaderNodeSum;
        }

        public void setPreNotExtensibleHeaderNodeSum(int preNotExtensibleHeaderNodeSum) {
            this.preNotExtensibleHeaderNodeSum = preNotExtensibleHeaderNodeSum;
        }

        public int getSubNotExtensibleHeaderNodeSum() {
            return subNotExtensibleHeaderNodeSum;
        }

        public void setSubNotExtensibleHeaderNodeSum(int subNotExtensibleHeaderNodeSum) {
            this.subNotExtensibleHeaderNodeSum = subNotExtensibleHeaderNodeSum;
        }

        @Override
        public String toString() {
            return "HeaderNode{" +
                    "headerName='" + headerName + '\'' +
                    ", index=" + index +
                    ", level=" + level +
                    ", parentIndex=" + parentIndex +
                    ", extensible=" + extensible +
                    ", preNotExtensibleHeaderNodeSum=" + preNotExtensibleHeaderNodeSum +
                    ", subNotExtensibleHeaderNodeSum=" + subNotExtensibleHeaderNodeSum +
                    '}';
        }
    }
}