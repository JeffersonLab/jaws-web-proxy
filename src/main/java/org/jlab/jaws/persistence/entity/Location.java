package org.jlab.jaws.persistence.entity;

import org.jlab.jaws.model.Node;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "LOCATION", schema = "JAWS_OWNER")
public class Location implements Serializable, Node {
    private static final long serialVersionUID = 1L;

    public static final BigInteger TREE_ROOT = BigInteger.ZERO;

    @Id
    @SequenceGenerator(name = "LocationId", sequenceName = "LOCATION_ID", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "LocationId")
    @Basic(optional = false)
    @NotNull
    @Column(name = "LOCATION_ID", nullable = false, precision = 22, scale = 0)
    private BigInteger locationId;
    @Size(max = 64)
    @Column(length = 64)
    private String name;
    private BigInteger weight;
    @JoinColumn(name = "PARENT_ID", referencedColumnName = "LOCATION_ID", nullable = true)
    @ManyToOne(optional = true)
    private Location parent;
    @OneToMany(mappedBy = "parent")
    @OrderBy("weight ASC, name ASC")
    private List<Location> childList;
    public BigInteger getLocationId() {
        return locationId;
    }

    public void setLocationId(BigInteger locationId) {
        this.locationId = locationId;
    }
    public String getName() {
        return name;
    }

    @Override
    public BigInteger getId() {
        return getLocationId();
    }

    @Override
    public List<? extends Node> getChildren() {
        return childList;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getParent() {
        return parent;
    }

    public void setParent(Location parent) {
        this.parent = parent;
    }

    public BigInteger getWeight() {
        return weight;
    }

    public void setWeight(BigInteger weight) {
        this.weight = weight;
    }

    public List<Location> getChildList() {
        return childList;
    }

    public void setChildList(List<Location> childList) {
        this.childList = childList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Location)) return false;
        Location entity = (Location) o;
        return Objects.equals(name, entity.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String toTreeString() {
        StringBuilder builder = new StringBuilder();

        toTreeString(builder, 0);

        return builder.toString();
    }

    public void toTreeString(StringBuilder builder, int indent) {

        String indentStr = "";

        if(indent > 0 && indent < 50) {
            for (int i = 0; i < indent; i++) {
                indentStr = indentStr + " ";
            }
        }

        builder.append(indentStr);
        builder.append(name);
        builder.append("\n");

        for(Location child: childList) {
            child.toTreeString(builder, indent + 1);
        }
    }
}
