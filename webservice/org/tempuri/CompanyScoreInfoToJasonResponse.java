
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
 *         &lt;element name="CompanyScoreInfoToJasonResult" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
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
    "companyScoreInfoToJasonResult"
})
@XmlRootElement(name = "CompanyScoreInfoToJasonResponse")
public class CompanyScoreInfoToJasonResponse {

    @XmlElement(name = "CompanyScoreInfoToJasonResult")
    protected String companyScoreInfoToJasonResult;

    /**
     * Gets the value of the companyScoreInfoToJasonResult property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCompanyScoreInfoToJasonResult() {
        return companyScoreInfoToJasonResult;
    }

    /**
     * Sets the value of the companyScoreInfoToJasonResult property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCompanyScoreInfoToJasonResult(String value) {
        this.companyScoreInfoToJasonResult = value;
    }

}
