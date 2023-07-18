package com.example;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
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
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.parser.api.YangParser;
import org.opendaylight.yangtools.yang.parser.impl.DefaultYangParserFactory;

import java.io.*;
import java.util.Collection;

public class Main {

	private static final QNameModule SE_MODULE = QNameModule.create(XMLNamespace.of("http://accedian.com/ns/yang/service/endpoint"), Revision.of("2023-06-23"));
	static final QName QNAME_SE = QName.create(SE_MODULE, "service-endpoints");
	static final QName QNAME_SE_LIST = QName.create(SE_MODULE, "service-endpoint");


	static EffectiveModelContext context;
	static JSONCodecFactory codecFactory;

	public static void main(String[] args) throws Exception {

		// one way to load yang model
		// NOTE !!!!!
		// this method creates an error when use the BindingContext to convert a node to its generated object
		String yangDirectory = "/Users/dsabag/dev/accedian/yang-model/src/main/yang";
		context = loadYangFromFiles(yangDirectory);

		// or another way
		//context = loadYangFromGeneratedSource();

		// build codec factory from context
		codecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.getShared(context);

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


			//
			// build a JsonParser object to be used for the payload
			//

			final var result = new NormalizedNodeResult();
			final var streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
			final var jsonParser = JsonParserStream.create(streamWriter,codecFactory,SchemaInferenceStack.Inference.ofDataTreePath(context,QNAME_SE));

			//
			// build a NormalizedNode that represents only the list of the yang container
			//
			jsonParser.parse(new JsonReader(new StringReader("""
				{
				  "Accedian-service-endpoint:service-endpoint": [
					{
					  "endpoint-id": "bcbb985-siteA",
					  "endpoint-name": "Site-A",
					  "description": "Session sender",
					  "type": "Accedian-service-endpoint-type:ne-endpoint"
					},
					{
					  "endpoint-id": "efc5f8-siteB",
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
			ContainerNode containerNode =
				ImmutableContainerNodeBuilder.create().withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(QNAME_SE)).withChild(
					Builders.mapBuilder().withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(QNAME_SE_LIST))
						.withValue((Collection<MapEntryNode>) node.body()).build()).build();

			System.out.println("\n---------- wrapped with container ----------------------------------");

			System.out.println(toJSON(containerNode));

			NormalizedNode nn = containerNode;
			System.out.println(nn.prettyTree());


		}catch (Exception e){
			e.printStackTrace();
		}
	}


	private static String toJSON(final NormalizedNode node) throws IOException {
		final Writer writer = new StringWriter();
		JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer,2);
		final NormalizedNodeStreamWriter jsonStream = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
						codecFactory, jsonWriter);
		try (NormalizedNodeWriter nodeWriter = NormalizedNodeWriter.forStreamWriter(jsonStream)) {
			nodeWriter.write(node);
		}
		return writer.toString();
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



}
