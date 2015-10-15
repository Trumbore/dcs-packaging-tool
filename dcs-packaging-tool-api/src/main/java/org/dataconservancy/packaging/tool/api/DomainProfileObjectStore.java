package org.dataconservancy.packaging.tool.api;

import java.net.URI;
import java.util.List;

import org.dataconservancy.packaging.tool.model.dprofile.NodeType;
import org.dataconservancy.packaging.tool.model.dprofile.PropertyType;
import org.dataconservancy.packaging.tool.model.dprofile.PropertyValue;
import org.dataconservancy.packaging.tool.model.ipm.Node;

/**
 * Maintain a set of domain objects described by a domain profile and allow
 * their properties to be manipulated.
 */
public interface DomainProfileObjectStore {
    /**
     * Modify the domain object of a node such that it is consistent with the
     * domain profiles specified by the node. The domain object of the parent
     * node already exist.
     * 
     * If the domain object already exists, any existing types on the object
     * will be removed. Any existing properties of the domain object will be
     * kept, although they may mean the object is not valid with respect to the
     * domain profiles specified in the node. Any existing relations between the
     * domain object and its parent will be removed.
     * 
     * If the node does not have a domain object, create a new object and set
     * it's identifier on the node. Structural relations between the node and
     * its parent will be set as specified by the primary domain profile of the
     * node. Default properties and system supplied properties will be added
     * from each domain profile of the node.
     * 
     * @param node
     *            The node to update.
     */
    void updateObject(Node node);

    /**
     * Add a property to a domain object.
     * 
     * @param object
     *            The identifier of the object to modify.
     * @param value
     *            The value to add.
     */
    void addProperty(URI object, PropertyValue value);

    /**
     * Remove a particular property from a object.
     * 
     * @param object
     *            The identifier of the object to modify.
     * @param value
     *            The value to remove.
     */
    void removeProperty(URI object, PropertyValue value);

    /**
     * Remove all properties of a given type from a object.
     * 
     * @param object
     *            The identifier of the object to modify.
     * @param type
     *            The type of the properties to remove.
     */
    void removeProperty(URI object, PropertyType type);

    /**
     * @param object
     *            The identifier of an object.
     * @param type
     *            The node type which the object corresponds to.
     * @return All properties of an object specified by a node type.
     */
    List<PropertyValue> getProperties(URI object, NodeType type);

    /**
     * @param object
     *            The identifier of an object.
     * @param type
     *            The type of properties to retrieve.
     * @return All properties of an object specified of the given type.
     */
    List<PropertyValue> getProperties(URI object, PropertyType type);

    /**
     * Check for the existence of a relationship.
     * 
     * @param subject
     *            The identifier of an object.
     * @param predicate
     *            The identifier of a predicate.
     * @param object
     *            The identifier of an object.
     * @return Whether or not the relationship exists.
     */
    boolean hasRelationship(URI subject, URI predicate, URI object);
}
