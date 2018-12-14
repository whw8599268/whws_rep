
package org.tempuri;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ScoreType" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Year" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="Quarter" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "scoreType",
    "year",
    "quarter"
})
@XmlRootElement(name = "CompanyAvgScoreInfo")
public class CompanyAvgScoreInfo {

    @XmlElement(name = "ScoreType")
    protected String scoreType;
    @XmlElement(name = "Year")
    protected String year;
    @XmlElement(name = "Quarter")
    protected String quarter;

    /**
     * Gets the value of the scoreType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getScoreType() {
        return scoreType;
    }

    /**
     * Sets the value of the scoreType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setScoreType(String value) {
        this.scoreType = value;
    }

    /**
     * Gets the value of the year property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getYear() {
        return year;
    }

    /**
     * Sets the value of the year property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setYear(String value) {
        this.year = value;
    }

    /**
     * Gets the value of the quarter property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getQuarter() {
        return quarter;
    }

    /**
     * Sets the value of the quarter property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setQuarter(String value) {
        this.quarter = value;
    }

}
