package org.dataconservancy.packaging.tool.impl;

/*
 * Copyright 2015 Johns Hopkins University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.dataconservancy.packaging.tool.api.DomainProfileStore;
import org.dataconservancy.packaging.tool.model.PackageResourceMapConstants;
import org.dataconservancy.packaging.tool.model.RDFTransformException;
import org.dataconservancy.packaging.tool.model.dprofile.NodeType;
import org.dataconservancy.packaging.tool.model.ipm.FileInfo;
import org.dataconservancy.packaging.tool.model.ipm.Node;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IpmRdfTransformService implements PackageResourceMapConstants  {

    public static final String DC_IPM_NS_URI = "http://dataconservancy.org/internal-package-model/";
    public static final Resource IPM_NODE_TYPE = ResourceFactory.createResource(DC_IPM_NS_URI + "IPMNode");
    public static final Resource FILE_INFO_TYPE = ResourceFactory.createResource(DC_IPM_NS_URI + "FileInfo");
    public static final Property HAS_PARENT = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasParent");
    public static final Property HAS_CHILD = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasChild");
    public static final Property HAS_FILE_INFO = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasFileInfo");
    public static final Property HAS_DOMAIN_OBJECT = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasDomainObject");
    public static final Property HAS_SUB_TYPE = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasSubType");
    public static final Property HAS_NODE_TYPE = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasNodeType");
    public static final Property HAS_LOCATION = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasLocation");
    public static final Property HAS_FORMAT = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasFormat");
    public static final Property HAS_SHA1_CHECKSUM = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasSHA1Checksum");
    public static final Property HAS_MD5_CHECKSUM = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasMD5Checksum");
    public static final Property HAS_SIZE = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasSize");
    public static final Property IS_DIRECTORY = ResourceFactory.createProperty(DC_IPM_NS_URI, "isDirectory");
    public static final Property HAS_CREATED_DATE = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasCreatedDate");
    public static final Property HAS_MODIFIED_DATE = ResourceFactory.createProperty(DC_IPM_NS_URI, "hasModifiedDate");
    public static final Property IS_ROOT = ResourceFactory.createProperty(DC_IPM_NS_URI, "isRoot");

    private DomainProfileStore domainProfileStore;

    public void setDomainProfileStore(DomainProfileStore profileStore) {
        this.domainProfileStore = profileStore;
    }

    public Model transformToRDF(Node node) throws RDFTransformException {
        //Create the basic model that will hold the RDF graph
        Model nodeModel = ModelFactory.createDefaultModel();

        createNodeResource(nodeModel, node);

        return nodeModel;
    }

    private Resource createNodeResource(Model nodeModel, Node node)
        throws RDFTransformException {
        //Create a resource for the node object
        Resource nodeResource = nodeModel.createResource();
        nodeResource.addProperty(RDF.type, IPM_NODE_TYPE);

        if (node.getIdentifier() != null) {
            nodeResource.addProperty(HAS_ID, node.getIdentifier().toString());
        }

        if (node.getNodeType() != null && node.getNodeType().getIdentifier() != null) {
            nodeResource.addProperty(HAS_NODE_TYPE, node.getNodeType().getIdentifier().toString());
        }

        if (node.getSubNodeTypes() != null) {
            for (NodeType subType : node.getSubNodeTypes()) {
                nodeResource.addProperty(HAS_SUB_TYPE, subType.getIdentifier().toString());
            }
        }

        if (node.getParent() == null) {
            nodeResource.addLiteral(IS_ROOT, true);
        } else if (node.getParent().getIdentifier() != null){
            List<Resource> parentList = nodeModel.listResourcesWithProperty(HAS_ID, node.getParent().getIdentifier().toString()).toList();
            //If the node isn't returned by the search create the parent
            if (parentList == null || parentList.isEmpty()) {
                nodeResource.addProperty(HAS_PARENT, createNodeResource(nodeModel, node.getParent()));
            } else {
                if (parentList.size() > 1) {
                    throw new RDFTransformException("Expected there to be only one node resource with id: " + node.getParent().getIdentifier());
                } else {
                    nodeResource.addProperty(HAS_PARENT, parentList.get(0));
                }
            }
        }

        if (node.getChildren() != null) {
            ArrayList<RDFNode> childNodes = new ArrayList<>();
            for (Node child : node.getChildren()) {
                if (child.getIdentifier() != null) {
                    List<Resource> childList = nodeModel.listResourcesWithProperty(HAS_ID, child.getIdentifier().toString()).toList();
                    //If the node isn't returned by the search create the child
                    if (childList == null || childList.isEmpty()) {
                        Resource childResource = createNodeResource(nodeModel, child);
                        childNodes.add(childResource);
                    } else {
                        if (childList.size() > 1) {
                            throw new RDFTransformException(
                                "Expected there to be only one node resource with id: " +
                                    child.getIdentifier());
                        } else {
                            childNodes.add(childList.get(0));
                        }
                    }
                }
            }

            RDFNode[] nodeArray = new RDFNode[childNodes.size()];
            nodeResource.addProperty(HAS_CHILD, nodeModel.createList(childNodes.toArray(nodeArray)));
        }

        nodeResource.addLiteral(IS_IGNORED, node.isIgnored());

        if (node.getDomainObject() != null) {
            nodeResource.addProperty(HAS_DOMAIN_OBJECT, node.getDomainObject().toString());
        }
        if (node.getFileInfo() != null) {
            nodeResource.addProperty(HAS_FILE_INFO, transformToRDF(node.getFileInfo(), nodeModel));
        }

        return nodeResource;
    }

    public Node transformToNode(Model model)
        throws RDFTransformException {

        Resource rootResource;
        ResIterator nodeIterator = model.listResourcesWithProperty(IS_ROOT);
        if (!nodeIterator.hasNext()) {
            throw new RDFTransformException("Expected at least one root node.");
        } else {
            rootResource = nodeIterator.next();
        }

        Node rootNode = null;
        if (rootResource != null) {
            rootNode = transformNodeFromResource(model, rootResource, null);
        }
        return rootNode;
    }

    private Node transformNodeFromResource(Model model,
                                                  Resource nodeResource,
                                                  Node parent)
        throws RDFTransformException {
        Node node = null;
        if (nodeResource != null) {
            if (nodeResource.hasProperty(HAS_ID)) {
                try {
                    node = new Node(new URI(getLiteral(nodeResource, HAS_ID)));
                } catch (URISyntaxException e) {
                    throw new RDFTransformException("Expected node id to be a uri", e);
                }
            }
        }

        if (node != null) {
            if (nodeResource.hasProperty(HAS_NODE_TYPE)) {
                try {
                    node.setNodeType(domainProfileStore.getNodeType(new URI(getLiteral(nodeResource, HAS_NODE_TYPE))));
                } catch (URISyntaxException e) {
                    throw new RDFTransformException("Expected node type to be a uri", e);
                }
            }

            if (nodeResource.hasLiteral(IS_IGNORED, true)) {
                node.setIgnored(true);
            } else {
                node.setIgnored(false);
            }

            if (nodeResource.hasProperty(HAS_DOMAIN_OBJECT)) {
                try {
                    node.setDomainObject(new URI(getLiteral(nodeResource, HAS_DOMAIN_OBJECT)));
                } catch (URISyntaxException e) {
                    throw new RDFTransformException("Expected domain object id to be a uri", e);
                }
            }

            if (nodeResource.hasProperty(HAS_FILE_INFO)) {
                List<RDFNode> fileInfoNodes = model.listObjectsOfProperty(nodeResource, HAS_FILE_INFO).toList();

                if (fileInfoNodes.size() != 1) {
                    throw new RDFTransformException("Expected only one instance of File info for node " + node.getIdentifier());
                }

                RDFNode fileInfoNode = fileInfoNodes.get(0);
                if (!fileInfoNode.isResource()) {
                    throw new RDFTransformException("Expected file info node to be a resource");
                }

                node.setFileInfo(transformToFileInfo(fileInfoNode.asResource()));
            }

            if (nodeResource.hasProperty(HAS_PARENT) && parent != null) {
                node.setParent(parent);

            }

            if (nodeResource.hasProperty(HAS_CHILD)) {
                List<RDFNode> rootChild = model.listObjectsOfProperty(nodeResource, IpmRdfTransformService.HAS_CHILD).toList();

                RDFList childList = rootChild.get(0).as(RDFList.class);

                ExtendedIterator<RDFNode> children = childList.iterator();
                while ( children.hasNext() ) {
                    RDFNode child = children.next().asResource();
                    if (!child.isResource()) {
                        throw new RDFTransformException("Expected child node to be a resource");
                    }
                    node.addChild(transformNodeFromResource(model, child.asResource(), node));
                }
            }

            if (nodeResource.hasProperty(HAS_SUB_TYPE)) {
                for (Statement stmt : nodeResource.listProperties(HAS_SUB_TYPE).toList()) {
                    String value = stmt.getLiteral().getString();
                    try {
                        node.addSubNodeType(domainProfileStore.getNodeType(new URI(value)));
                    } catch (URISyntaxException e) {
                        throw new RDFTransformException("Expected sub type to be a uri: " + value);
                    }
                }
            }
        }
        return node;
    }

    public Resource transformToRDF(FileInfo info, Model model) {
        Resource fileInfoResource = model.createResource();
        fileInfoResource.addProperty(RDF.type, FILE_INFO_TYPE);
        fileInfoResource.addProperty(HAS_NAME, info.getName());
        if (info.getLocation() != null) {
            fileInfoResource.addProperty(HAS_LOCATION, info.getLocation().toString());
        }
        fileInfoResource.addLiteral(HAS_SIZE, info.getSize());
        fileInfoResource.addLiteral(IS_BYTE_STREAM, info.isFile());
        fileInfoResource.addLiteral(IS_DIRECTORY, info.isDirectory());

        if (info.getFormats() != null) {
            for (String format : info.getFormats()) {
                fileInfoResource.addProperty(HAS_FORMAT, format);
            }
        }

        if (info.getChecksum(FileInfo.Algorithm.SHA1) != null) {
            fileInfoResource.addProperty(HAS_SHA1_CHECKSUM, info.getChecksum(FileInfo.Algorithm.SHA1));
        }

        if (info.getChecksum(FileInfo.Algorithm.MD5) != null) {
            fileInfoResource.addProperty(HAS_MD5_CHECKSUM, info.getChecksum(FileInfo.Algorithm.MD5));
        }

        if (info.getCreationTime() != null) {
            fileInfoResource.addLiteral(HAS_CREATED_DATE, info.getCreationTime().toMillis());
        }

        if (info.getLastModifiedTime() != null) {
            fileInfoResource.addLiteral(HAS_MODIFIED_DATE, info.getLastModifiedTime().toMillis());
        }
        return fileInfoResource;
    }

    public FileInfo transformToFileInfo(Resource fileInfoResource)
        throws RDFTransformException {
        FileInfo fileInfo = null;
        if (fileInfoResource != null) {
            URI location = null;
            if (fileInfoResource.hasProperty(HAS_LOCATION)) {
                location = URI.create(getLiteral(fileInfoResource, HAS_LOCATION));
            }

            String name = null;
            if (fileInfoResource.hasProperty(HAS_NAME)) {
                name = getLiteral(fileInfoResource, HAS_NAME);
            }

            boolean isFile = true;
            if (fileInfoResource.hasLiteral(IS_DIRECTORY, true)) {
                isFile = false;
            }

            String sha1Checksum = null;
            if (fileInfoResource.hasProperty(HAS_SHA1_CHECKSUM)) {
                sha1Checksum = getLiteral(fileInfoResource, HAS_SHA1_CHECKSUM);
            }

            String md5Checksum = null;
            if (fileInfoResource.hasProperty(HAS_MD5_CHECKSUM)) {
                md5Checksum = getLiteral(fileInfoResource, HAS_MD5_CHECKSUM);
            }
            Map<FileInfo.Algorithm, String> checksumMap = null;
            if (md5Checksum != null || sha1Checksum != null) {
                checksumMap = new HashMap<>();
                checksumMap.put(FileInfo.Algorithm.SHA1, sha1Checksum);
                checksumMap.put(FileInfo.Algorithm.MD5, md5Checksum);
            }

            List<String> formats = null;
            List<Statement> stmts = fileInfoResource.listProperties(HAS_FORMAT).toList();
            if (stmts != null && !stmts.isEmpty()) {
                formats = new ArrayList<>();
                for (Statement stmt : stmts) {
                    String value = stmt.getLiteral().getString();
                    formats.add(value);
                }
            }

            long size = -1;
            if (fileInfoResource.hasProperty(HAS_SIZE)) {
                size = Long.valueOf(getLiteral(fileInfoResource, HAS_SIZE));
            }

            FileTime createdDate = null;
            if (fileInfoResource.hasProperty(HAS_CREATED_DATE)) {
                createdDate = FileTime.fromMillis(Long.valueOf(getLiteral(fileInfoResource, HAS_CREATED_DATE)));
            }

            FileTime modifiedDate = null;
            if (fileInfoResource.hasProperty(HAS_MODIFIED_DATE)) {
                modifiedDate = FileTime.fromMillis(Long.valueOf(getLiteral(fileInfoResource, HAS_MODIFIED_DATE)));
            }

            fileInfo = new FileInfo(location, name);
            fileInfo.setSize(size);
            fileInfo.setCreationTime(createdDate);
            fileInfo.setLastModifiedTime(modifiedDate);
            fileInfo.setIsDirectory(!isFile);
            fileInfo.setIsFile(isFile);
            fileInfo.setChecksums(checksumMap);
            fileInfo.setFormats(formats);
        }

        return fileInfo;
    }

    private String getLiteral(Resource res, Property p) throws RDFTransformException {
        if (!res.hasProperty(p)) {
            throw new RDFTransformException("Expected node " + res + " to have property " + p);
        }

        RDFNode value = res.getProperty(p).getObject();

        if (!value.isLiteral()) {
            throw new RDFTransformException("Expected node " + res + " property " + p
                    + " to be a literal");
        }

        return value.asLiteral().getString();
    }
}
