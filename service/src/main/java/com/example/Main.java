package com.example;

import com.google.common.io.ByteSource;
import com.google.gson.stream.JsonReader;
import org.opendaylight.yangtools.yang.binding.YangModuleInfo;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.builder.DataContainerNodeBuilder;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactorySupplier;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.Builders;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.model.api.EffectiveModelContext;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.repo.api.SourceIdentifier;
import org.opendaylight.yangtools.yang.model.repo.api.YangTextSchemaSource;
import org.opendaylight.yangtools.yang.model.util.SchemaInferenceStack;
import org.opendaylight.yangtools.yang.parser.api.YangSyntaxErrorException;
import org.opendaylight.yangtools.yang.parser.rfc7950.reactor.RFC7950Reactors;
import org.opendaylight.yangtools.yang.parser.rfc7950.repo.YangStatementStreamSource;
import org.opendaylight.yangtools.yang.parser.stmt.reactor.CrossSourceStatementReactor;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class Main {


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


	private static List<YangStatementStreamSource> getYangStatementsFromYangModulesInfo(final Set<YangModuleInfo> yangModulesInfo)
					throws YangSyntaxErrorException, IOException {

		final ArrayList<YangStatementStreamSource> sourceArrayList = new ArrayList<>();
		for (YangModuleInfo yangModuleInfo : yangModulesInfo) {
			ByteSource byteSource = yangModuleInfo.getYangTextByteSource();
			SourceIdentifier sourceIdentifier = YangTextSchemaSource.identifierFromFilename(yangModuleInfo.getName().getLocalName() + ".yang");
			YangTextSchemaSource yangTextSchemaSource = YangTextSchemaSource.delegateForByteSource(sourceIdentifier,byteSource);
			YangStatementStreamSource statementSource = YangStatementStreamSource.create(yangTextSchemaSource);
			sourceArrayList.add(statementSource);
		}
		return sourceArrayList;
	}


	public static void try1(String payloadFile){
		try {

			Set<YangModuleInfo> mavenModelPaths = new HashSet<>(ACCEDIAN_MODELS);
			CrossSourceStatementReactor.BuildAction buildAction = RFC7950Reactors.defaultReactorBuilder().build().newBuild();

			buildAction.addSources(getYangStatementsFromYangModulesInfo(mavenModelPaths));
			EffectiveModelContext effectiveModelContext = buildAction.buildEffective();


			FileReader inputData = new FileReader(payloadFile);
			NormalizedNode deserializedNode = deserialize(effectiveModelContext, inputData);
			System.out.println("deserializedNode = " + deserializedNode);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	public static NormalizedNode deserialize(EffectiveModelContext effectiveModelContext, Reader inputData) throws IOException {

		JSONCodecFactory jsonCodecFactory = JSONCodecFactorySupplier.DRAFT_LHOTKA_NETMOD_YANG_JSON_02.createLazy(effectiveModelContext);
		SchemaInferenceStack.Inference inference = SchemaInferenceStack.of(jsonCodecFactory.getEffectiveModelContext()).toInference();

		final DataContainerNodeBuilder<YangInstanceIdentifier.NodeIdentifier, ContainerNode> resultBuilder = Builders.containerBuilder()
						.withNodeIdentifier(YangInstanceIdentifier.NodeIdentifier.create(SchemaContext.NAME));
		NormalizedNodeStreamWriter writer = ImmutableNormalizedNodeStreamWriter.from(resultBuilder);

		try (JsonReader reader = new JsonReader(inputData);
						JsonParserStream jsonParser = JsonParserStream.create(writer, jsonCodecFactory, inference)) {
			jsonParser.parse(reader);
		}

		return resultBuilder.build();
	}




	public static void main(String[] args) {
		System.out.println("Correct Payload:");
		try1("./src/main/resources/correct-payload.json");

		System.out.println("Incorrect Payload:");
		try1("./src/main/resources/incorrect-payload.json");
	}

}
