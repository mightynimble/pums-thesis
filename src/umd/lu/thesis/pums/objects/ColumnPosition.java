package umd.lu.thesis.pums.objects;

import java.util.logging.Level;
import java.util.logging.Logger;
import umd.lu.thesis.helper.ExcelUtils;

/**
 *
 * @author Home
 */
public class ColumnPosition {

    private int start;

    private int end;

    private String type;

    private int length;

    private String name;

    private String description;

    public ColumnPosition(String line) {
        try {
            type = ExcelUtils.getColumnValue(ExcelUtils.a, line);
            start = Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.b, line));
            end = Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.c, line));
            length = Integer.parseInt(ExcelUtils.getColumnValue(ExcelUtils.d, line));
            name = ExcelUtils.getColumnValue(ExcelUtils.f, line);
            description = ExcelUtils.getColumnValue(ExcelUtils.g, line);
        }
        catch (Exception ex) {
            System.out.println(ex);
        }
    }

    /**
     * @return the start
     */
    public int getStart() {
        return start;
    }

    /**
     * @param start the start to set
     */
    public void setStart(int start) {
        this.start = start;
    }

    /**
     * @return the end
     */
    public int getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(int end) {
        this.end = end;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the length
     */
    public int getLength() {
        return length;
    }

    /**
     * @param length the length to set
     */
    public void setLength(int length) {
        this.length = length;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
