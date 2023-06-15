package com.example;

import com.google.common.io.ByteSource;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.endpoint.rev221025.ServiceEndpoints;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.common.QNameModule;
import org.opendaylight.yangtools.yang.common.Revision;
import org.opendaylight.yangtools.yang.common.XMLNamespace;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.*;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.tree.api.*;
import org.opendaylight.yangtools.yang.data.tree.impl.di.InMemoryDataTreeFactory;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.stmt.SchemaNodeIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.parser.api.YangParser;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.EffectiveSchemaContext;

import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Main {

	private static final QNameModule SE_MODULE = QNameModule.create(XMLNamespace.of("http://accedian.com/ns/yang/service/endpoint"), Revision.of("2022-10-25"));
	static final QName QNAME_SE = QName.create(SE_MODULE, "service-endpoints");
	static final QName QNAME_SE_LIST = QName.create(SE_MODULE, "service-endpoint");

    // currently this is not needed since i load all yang files in the directory
    // left here for possible use in future
    public static final Set<YangModuleInfo> ACCEDIAN_MODELS = Set.of(
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.extensions.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.endpoint.ne.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.endpoint.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.endpoint.type.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.l3vpn.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.service.type.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.session.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.session.twamp.light.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.session.type.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.http.accedian.com.ns.yang.types.rev221025.$YangModuleInfoImpl.getInstance(),
        org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.$YangModuleInfoImpl.getInstance()
    );



	public static void main(String[] args) {

		try4();

	}


	/*
	this shows an example of all steps from json payload into a generated DTO

	1. build an EffectiveModelContext from yang files
	2. build a JsonParser object to be used for the payload
	3. build a NormalizedNode that represents only the list of the yang container
	4. wrap the Map of ServiceEndpoint inside a ContainerNode
	5. build a binding context
	6. convert the combined node to a Java Generated Object

	 */
	private static void try4() {

		//
		// build an EffectiveModelContext from yang files
		//
		try {

			// one way to load yang model
			// NOTE !!!!!
			// this method creates an error when use the BindingContext to convert a node to its generated object
			//String yangDirectory = "/Users/dsabag/dev/yang-example/yang-model/src/main/yang/";
			//EffectiveModelContext context = loadYangFromFiles(yangDirectory);

			// or another way
			EffectiveModelContext context = loadYangFromGeneratedSource();


			// build codec factory from context
			JSONCodecFactory codecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(context);


			//
			// build a JsonParser object to be used for the payload
			//

			final var result = new NormalizedNodeResult();
			final var streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
			final var jsonParser = JsonParserStream.create(
							streamWriter,
							codecFactory,
							SchemaInferenceStack.Inference.ofDataTreePath(context, QNAME_SE)
			);

			//
			// build a NormalizedNode that represents only the list of the yang container
			//
			jsonParser.parse(new JsonReader(new StringReader("""
			{
			  "Accedian-service-endpoint:service-endpoint": [
				{
				  "endpoint-id": "5bcbb985-siteA",
				  "endpoint-name": "Site-A",
				  "description": "Session sender",
				  "type": "Accedian-service-endpoint-type:ne-endpoint"
				},
				{
				  "endpoint-id": "92efc5f8-siteB",
				  "endpoint-name": "Site-B",
				  "description": "Session reflector",
				  "type": "Accedian-service-endpoint-type:ne-endpoint"
				}
			  ]
			}
            """)));
			final var node = result.getResult();

			//
			// wrap the Map of ServiceEndpoint inside a ContainerNode
			//
			ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
				.withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(QNAME_SE))
				.withChild(
					Builders.mapBuilder()
						.withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(QNAME_SE_LIST))
						.withValue((Collection<MapEntryNode>) node.body())
						.build()
				)
				.build();

			System.out.println("\n---------- wrapped with container ----------------------------------");
			System.out.println(containerNode.prettyTree());
			System.out.println(toJSON(codecFactory, containerNode));


			//
			// create data tree
			//
			DataTree dataTree = new InMemoryDataTreeFactory().create(DataTreeConfiguration.DEFAULT_CONFIGURATION, context);


			//
			// save node to data tree
			//
			DataTreeSnapshot snapshot = dataTree.takeSnapshot();
			final DataTreeModification change1 = snapshot.newModification();
			change1.merge(YangInstanceIdentifier.of(QNAME_SE), containerNode);
			change1.ready();
			dataTree.validate(change1);
			final DataTreeCandidate prepare = dataTree.prepare(change1);
			dataTree.commit(prepare);


			snapshot = dataTree.takeSnapshot();

			//
			// find node using readNode() from snapshot
			//
			YangInstanceIdentifier SINGLE_SE_BY_ID = YangInstanceIdentifier
							.builder(YangInstanceIdentifier.of(QNAME_SE))
							.node(QNAME_SE_LIST)
							.nodeWithKey(QNAME_SE_LIST, QName.create(QNAME_SE, "endpoint-id"), "92efc5f8-siteB")
							.build();

			Optional<NormalizedNode> optNode = snapshot.readNode(SINGLE_SE_BY_ID);
			System.out.println("\n----------- snapshot.readNode() ---------------------------------");
			System.out.println("Find By ID:");

			// wrap it a map, so it will be converted to json with the proper root
			NormalizedNode nn =
					Builders.mapBuilder()
						.withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(QNAME_SE_LIST))
						.withChild((MapEntryNode)optNode.get())
						.build()
			;

			System.out.println(nn.prettyTree());
			SchemaInferenceStack.Inference inference = SchemaInferenceStack.of(context, SchemaNodeIdentifier.Absolute.of(QNAME_SE)).toInference();
			XMLNamespace xmlName = XMLNamespace.of("http://accedian.com/ns/yang/service/endpoint@2022-10-25#service-endpoint");
			System.out.println( serializeData(codecFactory, inference, xmlName, nn) );


			/* for now, not needed
			//
			// find node using StoreTreeNodes.findNode() with TreeNode
			//
			TreeNode treeNode = TreeNode.of(containerNode, Version.initial());
			// NOTE: when working with tree node find(), you need to remove the top container from the YangInstanceIdentifier
			YangInstanceIdentifier SE_BY_ID = YangInstanceIdentifier
							.builder(YangInstanceIdentifier.of(QNAME_SE_LIST))
							.nodeWithKey(QNAME_SE_LIST, QName.create(QNAME_SE, "endpoint-id"), "92efc5f8-siteB")
							.build();

			Optional<? extends TreeNode> treeNode1 = StoreTreeNodes.findNode(treeNode, SE_BY_ID);
			System.out.println("\n--------- StoreTreeNodes.findNode() -----------------------------------");
//			xmlName = XMLNamespace.of("http://accedian.com/ns/yang/service/endpoint@2022-10-25#service-endpoint[endpoint-id='92efc5f8-siteB']");
			NormalizedNode node3 = treeNode1.get().getData();
			xmlName = node3.getIdentifier().getNodeType().getNamespace();
			//TODO - FIXME to be able to convert a specific map entry to json
			inference = SchemaInferenceStack.of(context, SchemaNodeIdentifier.Absolute.of(QName.create(SE_MODULE, "service-endpoints"), QName.create(SE_MODULE, "service-endpoint") )).toInference();
			System.out.println(serializeData(codecFactory, inference, xmlName, node3));
			*/


			//
			// get all endpoints from snapshot
			//
			YangInstanceIdentifier yangIdAllList = YangInstanceIdentifier
							.builder(YangInstanceIdentifier.of(QNAME_SE))
							.build();
			Optional<NormalizedNode> node2 = snapshot.readNode(yangIdAllList);
			System.out.println("all list: \n" + node2.get().prettyTree());
			SchemaInferenceStack.Inference inference1 = SchemaInferenceStack.of(context).toInference();
			System.out.println("json: \n" + serializeData(codecFactory, inference1, SE_MODULE.getNamespace(), node2.get()));


			//
			// build a binding context
			//

//			final ModuleInfoSnapshotBuilder moduleInfoSnapshotBuilder = new ModuleInfoSnapshotBuilder(new DefaultYangParserFactory());
//			moduleInfoSnapshotBuilder.add(org.opendaylight.yang.gen.v1.urn.example2.norev.$YangModuleInfoImpl.getInstance());
//			final BindingRuntimeGenerator bindingRuntimeGenerator = new DefaultBindingRuntimeGenerator();
//			final BindingRuntimeTypes bindingRuntimeTypes = bindingRuntimeGenerator.generateTypeMapping(context);
//			DefaultBindingRuntimeContext bindingRuntimeContext = new DefaultBindingRuntimeContext(bindingRuntimeTypes, moduleInfoSnapshotBuilder.build());

			// faster and simpler way to construct BindingCodecContext
			BindingRuntimeContext bindingRuntimeContext = BindingRuntimeHelpers.createRuntimeContext();
			BindingCodecContext bindingCodecContext = new BindingCodecContext(bindingRuntimeContext);

			// for now, this is not needed
//			ConstantAdapterContext codec = new ConstantAdapterContext(bindingCodecContext);
//			BindingNormalizedNodeSerializer serializer = codec.currentSerializer();


			//
			// bind the combined node to a Java Generated Object
			//
			final YangInstanceIdentifier seYII = YangInstanceIdentifier.builder().node(ServiceEndpoints.QNAME).build();
			final Map.Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode = bindingCodecContext.fromNormalizedNode(seYII, containerNode);
			final ServiceEndpoints value = (ServiceEndpoints) fromNormalizedNode.getValue();
			System.out.println("\n--------- NormalizedNode to Generated object -----------------------------------");
			System.out.println("endpoints = " + value);

			//
			// now, convert back from "yang generated object" to json
			//
//			System.out.println("\n--------------------------------------------");
//			InstanceIdentifier<ServiceEndpoints> iise = InstanceIdentifier.create(ServiceEndpoints.class);
//			Map.Entry<YangInstanceIdentifier, NormalizedNode> nodeEntry = bindingCodecContext.toNormalizedNode(iise,value);
//			System.out.println(toJSON(codecFactory, nodeEntry.getValue()));

		} catch (Exception e) {
			e.printStackTrace();
		}


	}


	private static String toJSON(JSONCodecFactory codecFactory, final NormalizedNode node) throws IOException {
		final Writer writer = new StringWriter();
		JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer,2);
		final NormalizedNodeStreamWriter jsonStream = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
						codecFactory, jsonWriter);
		try (NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStream)) {
			nodeWriter.write(node);
		}
		return writer.toString();
	}


	public static Writer serializeData(JSONCodecFactory codecFactory, final SchemaInferenceStack.Inference inference, XMLNamespace initialNamespace,
					final NormalizedNode normalizedNode) {
		final Writer writer = new StringWriter();
//		final XMLNamespace initialNamespace = normalizedNode.getIdentifier().getNodeType().getNamespace();
		// nnStreamWriter closes underlying JsonWriter, we don't need too
		final JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer, 2);
		// Exclusive nnWriter closes underlying NormalizedNodeStreamWriter, we don't need too
		final boolean useNested = false;//normalizedNode instanceof MapEntryNode;
//		final NormalizedNodeStreamWriter nnStreamWriter = useNested
//						? JSONNormalizedNodeStreamWriter.createNestedWriter(codecFactory, inference, initialNamespace, jsonWriter)
//						: JSONNormalizedNodeStreamWriter.createExclusiveWriter(codecFactory, inference, initialNamespace, jsonWriter);
		final NormalizedNodeStreamWriter nnStreamWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(codecFactory, inference, initialNamespace, jsonWriter);

		try (NormalizedNodeWriter nnWriter = NormalizedNodeWriter.forStreamWriter(nnStreamWriter)) {
			nnWriter.write(normalizedNode);
			return writer;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (useNested) {
				try {
					jsonWriter.close();
				} catch (IOException e) {
					System.out.println("Failed to close underlying JsonWriter");
					e.printStackTrace();
				}
			}
		}
		return null;
	}


	private static EffectiveModelContext loadYangFromFiles(String yangDirectory) throws Exception {
		File[] fileArray = new File(yangDirectory).listFiles((dir,name) -> name.endsWith(".yang"));
		YangParser parser = new DefaultYangParserFactory().createParser();
		for(File file : fileArray) {
			YangTextSchemaSource source = YangTextSchemaSource.forPath(file.toPath());
			parser.addSource(source);
		}
		return parser.buildEffectiveModel();
	}


	private static EffectiveSchemaContext loadYangFromGeneratedSource() throws Exception {
		// this is a different way to load yang files from their generated YangModuleInfo
        CrossSourceStatementReactor.BuildAction buildAction = RFC7950Reactors.defaultReactorBuilder().build().newBuild();
        for (YangModuleInfo yangModuleInfo : ACCEDIAN_MODELS) {
            ByteSource byteSource = yangModuleInfo.getYangTextByteSource();
            SourceIdentifier sourceIdentifier = YangTextSchemaSource.identifierFromFilename(yangModuleInfo.getName().getLocalName() + ".yang");
            YangTextSchemaSource yangTextSchemaSource = YangTextSchemaSource.delegateForByteSource(sourceIdentifier,byteSource);
            YangStatementStreamSource statementSource = YangStatementStreamSource.create(yangTextSchemaSource);
            buildAction.addSource(statementSource);
        }
		return buildAction.buildEffective();
	}
}
