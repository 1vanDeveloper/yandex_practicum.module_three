package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should get transfer history by login"
    request {
        method 'GET'
        url '/transfer/history/test_user'
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body(
                [
                        [
                                id: 1,
                                fromAccountLogin: "test_user",
                                toAccountLogin: "other_user",
                                amount: 100.00,
                                status: "COMPLETED"
                        ]
                ]
        )
    }
}
