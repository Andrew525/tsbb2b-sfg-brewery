package guru.springframework.brewery.web.controllers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import guru.springframework.brewery.services.BeerService;
import guru.springframework.brewery.web.model.BeerDto;
import guru.springframework.brewery.web.model.BeerPagedList;
import guru.springframework.brewery.web.model.BeerStyleEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BeerControllerTest {

    @Mock
    BeerService beerService;

    @InjectMocks
    BeerController controller;

    MockMvc mockMvc;

    BeerDto dto;

    @BeforeEach
    void setUp() {
        dto = BeerDto.builder()
                .id(UUID.randomUUID())
                .version(1)
                .beerName("TestName")
                .beerStyle(BeerStyleEnum.ALE)
                .price(new BigDecimal("12.99"))
                .quantityOnHand(4)
                .upc(1233L)
                .createdDate(OffsetDateTime.now())
                .lastModifiedDate(OffsetDateTime.now())
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(jackson2HttpMessageConverter()).build();
    }

    @Test
    void testGetBeerById() throws Exception {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        given(beerService.findBeerById(any())).willReturn(dto);

        MvcResult result = mockMvc.perform(get("/api/v1/beer/" + dto.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("$.id", is(dto.getId().toString())))
                .andExpect(jsonPath("$.beerName", is("TestName")))
                .andExpect(jsonPath("$.price", is("12.99")))
                .andExpect(jsonPath("$.createdDate", is(formatter.format(dto.getCreatedDate()))))
                .andReturn();

        System.out.println(result.getResponse().getContentAsString());
    }


    @Nested
    @DisplayName("List ops ...")
    public class TestListOperations {

        @Captor
        ArgumentCaptor<String> stringCaptor;

        @Captor
        ArgumentCaptor<BeerStyleEnum> styleCaptor;

        @Captor
        ArgumentCaptor<PageRequest> pageCaptor;

        BeerPagedList pagedList;

        @BeforeEach
        void setUp() {
            BeerDto dto2 = BeerDto.builder()
                    .id(UUID.randomUUID())
                    .version(1)
                    .beerName("Supper Beer")
                    .beerStyle(BeerStyleEnum.WHEAT)
                    .price(new BigDecimal("10.99"))
                    .quantityOnHand(10)
                    .upc(1233L)
                    .createdDate(OffsetDateTime.now())
                    .lastModifiedDate(OffsetDateTime.now())
                    .build();

            pagedList = new BeerPagedList(List.of(dto, dto2), PageRequest.of(1, 1), 2L);


            given(beerService.listBeers(stringCaptor.capture(), styleCaptor.capture(), pageCaptor.capture()))
                    .willReturn(pagedList);
        }

        @Test
        @DisplayName("test list beers -- no parameters")
        void testListBeers() throws Exception {

            mockMvc.perform(get("/api/v1/beer"))

                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(dto.getId().toString())));
        }

    }
    public MappingJackson2HttpMessageConverter jackson2HttpMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, true);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        objectMapper.registerModule(new JavaTimeModule());
        return new MappingJackson2HttpMessageConverter(objectMapper);
    }
}