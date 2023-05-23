package com.example;

import com.google.gson.stream.JsonReader;
import org.opendaylight.mdsal.binding.dom.codec.impl.BindingCodecContext;
import org.opendaylight.mdsal.binding.generator.impl.DefaultBindingRuntimeGenerator;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeGenerator;
import org.opendaylight.mdsal.binding.runtime.api.BindingRuntimeTypes;
import org.opendaylight.mdsal.binding.runtime.api.DefaultBindingRuntimeContext;
import org.opendaylight.mdsal.binding.runtime.spi.ModuleInfoSnapshotBuilder;
import org.opendaylight.yang.gen.v1.urn.example2.norev.ServiceEndpoints;
import org.opendaylight.yang.gen.v1.urn.example2.norev.service.endpoints.ServiceEndpoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.MapEntryNode;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.impl.schema.builder.impl.ImmutableMapNodeBuilder;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.parser.api.YangParser;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;

import java.io.File;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;

public class Main {


//	public static final Set<YangModuleInfo> ACCEDIAN_MODELS = Set.of(
//		org.opendaylight.yang.gen.v1.urn.example2.norev.$YangModuleInfoImpl.getInstance()
//	);


	public static void main(String[] args) {

		try4();

	}


	private static void try4() {

		String yangDirectory = "/Users/dsabag/dev/yang-example/yang-model/src/main/yang/";

		try {
			File[] fileArray = new File(yangDirectory).listFiles((dir,name) -> name.endsWith(".yang"));

			YangParser parser = new DefaultYangParserFactory().createParser();
			for(File file : fileArray) {
				YangTextSchemaSource source = YangTextSchemaSource.forPath(file.toPath());
				parser.addSource(source);
			}
			EffectiveModelContext context = parser.buildEffectiveModel();

			final QName CONT = QName.create("urn:example2", "service-endpoints");

			final var result = new NormalizedNodeResult();
			final var streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
			final var jsonParser = JsonParserStream.create(
							streamWriter,
							JSONCodecFactorySupplier.RFC7951.getShared(context),
//							JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(context),
							SchemaInferenceStack.Inference.ofDataTreePath(context, CONT)
			);

			jsonParser.parse(new JsonReader(new StringReader("""
			{
			  "Accedian-service-endpoint:service-endpoint": [
				{
				  "endpoint-id": "5bcbb985-ca2d-4d0b-ba0e-2617c5bfa70c",
				  "endpoint-name": "Site-A",
				  "description": "Session sender",
				  "type": "Accedian-service-endpoint-type:ne-endpoint"
				},
				{
				  "endpoint-id": "92efc5f8-d441-4fe5-a1af-13b0d4422895",
				  "endpoint-name": "Site-B",
				  "description": "Session reflector",
				  "type": "Accedian-service-endpoint-type:ne-endpoint"
				}
			  ]
			}
            """)));
			final var node = result.getResult();
			System.out.println("node = " + node);


			//
			// convert the node to generated object
			//

			final ModuleInfoSnapshotBuilder moduleInfoSnapshotBuilder = new ModuleInfoSnapshotBuilder(new DefaultYangParserFactory());
			moduleInfoSnapshotBuilder.add(org.opendaylight.yang.gen.v1.urn.example2.norev.$YangModuleInfoImpl.getInstance());

			final BindingRuntimeGenerator bindingRuntimeGenerator = new DefaultBindingRuntimeGenerator();
			final BindingRuntimeTypes bindingRuntimeTypes = bindingRuntimeGenerator.generateTypeMapping(context);
			DefaultBindingRuntimeContext bindingRuntimeContext = new DefaultBindingRuntimeContext(bindingRuntimeTypes, moduleInfoSnapshotBuilder.build());
			BindingCodecContext bindingCodecContext = new BindingCodecContext(bindingRuntimeContext);
//			ConstantAdapterContext codec = new ConstantAdapterContext(bindingCodecContext);
//			BindingNormalizedNodeSerializer serializer = codec.currentSerializer();



			final YangInstanceIdentifier contYII = YangInstanceIdentifier.builder().node(ServiceEndpoints.QNAME).build();

			ContainerNode containerNode = ImmutableContainerNodeBuilder.create()
							.withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(ServiceEndpoints.QNAME))
							.withChild(ImmutableMapNodeBuilder.create()
											.withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(ServiceEndpoint.QNAME))
											.withValue((Collection<MapEntryNode>) node.body())
											.build()
							)
							.build();

			System.out.println(containerNode.prettyTree());

			final Map.Entry<InstanceIdentifier<?>, DataObject> fromNormalizedNode = bindingCodecContext.fromNormalizedNode(contYII, containerNode);
			final ServiceEndpoints value = (ServiceEndpoints) fromNormalizedNode.getValue();
			System.out.println("endpoints = " + value);


		} catch (Exception e) {
			e.printStackTrace();
		}


	}

}
