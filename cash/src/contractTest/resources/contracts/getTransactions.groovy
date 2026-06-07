import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should get transactions by login"
    request {
        method 'GET'
        url '/cash/transactions/test_user'
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
                                accountLogin: "test_user",
                                transactionType: "DEPOSIT",
                                amount: 100.00,
                                status: "COMPLETED"
                        ],
                        [
                                id: 2,
                                accountLogin: "test_user",
                                transactionType: "WITHDRAW",
                                amount: 50.00,
                                status: "COMPLETED"
                        ]
                ]
        )
    }
}
