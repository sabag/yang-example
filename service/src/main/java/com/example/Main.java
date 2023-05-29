package com.example;

import com.google.gson.stream.JsonReader;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeContext;
import org.opendaylight.mdsal.binding.runtime.spi.BindingRuntimeHelpers;
import org.opendaylight.yang.gen.v1.urn.example2.norev.ServiceEndpoints;
import org.opendaylight.yang.gen.v1.urn.example2.norev.service.endpoints.ServiceEndpoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
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
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.parser.api.YangParser;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;

import java.io.*;
import java.util.Collection;
import java.util.Map;

public class Main {


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
		String yangDirectory = "/Users/dsabag/dev/yang-example/yang-model/src/main/yang/";

		try {
			File[] fileArray = new File(yangDirectory).listFiles((dir,name) -> name.endsWith(".yang"));

			YangParser parser = new DefaultYangParserFactory().createParser();
			for(File file : fileArray) {
				YangTextSchemaSource source = YangTextSchemaSource.forPath(file.toPath());
				parser.addSource(source);
			}
			EffectiveModelContext context = parser.buildEffectiveModel();

			// build codec factory from context
			JSONCodecFactory codecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(context);
			//							JSONCodecFactorySupplier.RFC7951.getShared(context),

			//
			// build a JsonParser object to be used for the payload
			//
			final QName CONT = QName.create("urn:example2", "service-endpoints");

			final var result = new NormalizedNodeResult();
			final var streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
			final var jsonParser = JsonParserStream.create(
							streamWriter,
							codecFactory,
							SchemaInferenceStack.Inference.ofDataTreePath(context, CONT)
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
				.withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(ServiceEndpoints.QNAME))
				.withChild(
					Builders.mapBuilder()
						.withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(ServiceEndpoint.QNAME))
						.withValue((Collection<MapEntryNode>) node.body())
						.build()
				)
				.build();

			System.out.println(containerNode.prettyTree());


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
			System.out.println("endpoints = " + value);

			//
			// now, convert back from "yang generated object" to json
			//
			System.out.println("--------------------------------------------");
			InstanceIdentifier<ServiceEndpoints> iise = InstanceIdentifier.create(ServiceEndpoints.class);
			Map.Entry<YangInstanceIdentifier, NormalizedNode> nodeEntry = bindingCodecContext.toNormalizedNode(iise,value);
			System.out.println(toJSON(codecFactory, nodeEntry.getValue()));

		} catch (Exception e) {
			e.printStackTrace();
		}


	}


	private static String toJSON(JSONCodecFactory codecFactory, final NormalizedNode input) throws IOException {
		final Writer writer = new StringWriter();
		final NormalizedNodeStreamWriter jsonStream = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
						codecFactory, JsonWriterFactory.createJsonWriter(writer, 2));
		try (NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStream)) {
			nodeWriter.write(input);
		}

		return writer.toString();
	}


}
